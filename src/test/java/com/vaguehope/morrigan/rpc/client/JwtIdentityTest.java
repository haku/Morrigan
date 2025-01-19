package com.vaguehope.morrigan.rpc.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import com.google.common.util.concurrent.MoreExecutors;

import io.grpc.CallCredentials.MetadataApplier;
import io.grpc.CallCredentials.RequestInfo;
import io.grpc.Metadata;
import io.grpc.Status;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.PrivateJwk;
import io.jsonwebtoken.security.PublicJwk;

public class JwtIdentityTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@SuppressWarnings("unused")
	@Test
	public void itReloadsKey() throws Exception {
		final File f = this.tmp.newFile();
		new JwtIdentity(f, "my-host-name");
		final String generatedKey = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
		assertThat(generatedKey.length(), greaterThan(1));

		final JwtIdentity undertest = new JwtIdentity(f, "my-host-name");
		final String loadedKey = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
		assertEquals(generatedKey, loadedKey);

		try {
			new JwtIdentity(f, "different-host-name");
			fail("Expected exception.");
		}
		catch (final IllegalStateException e) {
			assertThat(e.getMessage(), startsWith("keyId does not match"));
		}
	}

	@Test
	public void itAppliesToRequest() throws Exception {
		final RequestInfo requestInfo = mock(RequestInfo.class);
		final MetadataApplier applier = mock(MetadataApplier.class);

		final File file = this.tmp.newFile();
		final JwtIdentity undertest = new JwtIdentity(file, "my-host-name");
		undertest.applyRequestMetadata(requestInfo, MoreExecutors.directExecutor(), applier);
		undertest.applyRequestMetadata(requestInfo, MoreExecutors.directExecutor(), applier);

		verify(applier, times(0)).fail(any(Status.class));
		final ArgumentCaptor<Metadata> cap = ArgumentCaptor.forClass(Metadata.class);
		verify(applier, times(2)).apply(cap.capture());

		final String header1 = cap.getAllValues().get(0).get(JwtIdentity.AUTHORIZATION_METADATA_KEY);
		final String loadedKey = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
		final PublicJwk<?> pubKey = ((PrivateJwk<?, ?, ?>) Jwks.parser().build().parse(loadedKey)).toPublicJwk();
		final JwtParser parser = Jwts.parser().verifyWith(pubKey.toKey()).build();
		final Jws<Claims> jws = parser.parseSignedClaims(header1.substring(JwtIdentity.BEARER_TYPE.length()).trim());

		final Jwk<?> embeddedJwk = Jwks.parser().build().parse(jws.getPayload().get("jwk").toString());
		assertEquals(pubKey, embeddedJwk);

		final String header2 = cap.getAllValues().get(1).get(JwtIdentity.AUTHORIZATION_METADATA_KEY);
		final Jws<Claims> jws2 = parser.parseSignedClaims(header2.substring(JwtIdentity.BEARER_TYPE.length()).trim());
		assertEquals(null, jws2.getPayload().get("jwk"));
	}

}
