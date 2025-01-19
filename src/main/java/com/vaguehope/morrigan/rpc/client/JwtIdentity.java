package com.vaguehope.morrigan.rpc.client;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.Executor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.util.concurrent.RateLimiter;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.PrivateJwk;
import io.jsonwebtoken.security.PublicJwk;

public class JwtIdentity extends CallCredentials {

	static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
	static final String BEARER_TYPE = "Bearer";

	private final PrivateKey privateKey;
	private final PublicJwk<?> publicJwk;
	private final String username;

	private final RateLimiter sendPublicKeyRate = RateLimiter.create(1d / 300d);

	public JwtIdentity(final File keyFile, final String hostName) throws IOException {
		if (StringUtils.isBlank(hostName)) throw new IllegalArgumentException("hostName can not be blank.");
		this.username = "morrigan@" + hostName;

		if (!keyFile.exists() || keyFile.length() < 1) {
			generateKey(keyFile, this.username);
		}
		final PrivateJwk<?, ?, ?> privateJwk = loadKey(keyFile, this.username);
		this.privateKey = privateJwk.toKey();
		this.publicJwk = privateJwk.toPublicJwk();
	}

	private static void generateKey(final File keyFile, final String name) throws IOException {
		final java.security.KeyPair pair = Jwts.SIG.ES512.keyPair().build();
		final PrivateJwk<?, ?, ?> jwk = Jwks.builder()
				.keyPair(pair)
				.id(name)
				.build();
		FileUtils.write(keyFile, Jwks.UNSAFE_JSON(jwk), StandardCharsets.UTF_8);
	}

	private static PrivateJwk<?, ?, ?> loadKey(final File keyFile, final String name) throws IOException {
		final String raw = FileUtils.readFileToString(keyFile, StandardCharsets.UTF_8);
		final Jwk<?> key = Jwks.parser().build().parse(raw);
		if (!(key instanceof PrivateJwk)) throw new IllegalStateException("Does not contain a private key: " + keyFile);

		final PrivateJwk<?, ?, ?> privateJwk = (PrivateJwk<?, ?, ?>) key;
		if (!name.equals(privateJwk.getId())) {
			throw new IllegalStateException("keyId does not match: " + privateJwk.getId() + " != " + name);
		}

		return privateJwk;
	}

	@Override
	public void applyRequestMetadata(final RequestInfo requestInfo, final Executor appExecutor, final MetadataApplier applier) {
		final JwtBuilder jwtBuilder = Jwts.builder()
				.header().add("username", this.username).and()
				.expiration(Date.from(Instant.now().plusSeconds(30)))
				.subject(this.username);

		if (this.sendPublicKeyRate.tryAcquire()) {
			jwtBuilder.header().add("jwk", this.publicJwk);
		}

		final String jws = jwtBuilder
				.signWith(this.privateKey)
				.compact();

		appExecutor.execute(() -> {
			try {
				final Metadata headers = new Metadata();
				headers.put(AUTHORIZATION_METADATA_KEY, String.format("%s %s", BEARER_TYPE, jws));
				applier.apply(headers);
			}
			catch (final Throwable e) {
				applier.fail(Status.UNAUTHENTICATED.withCause(e));
			}
		});
	}

}
