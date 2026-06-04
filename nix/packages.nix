{ inputs, ... }:
{
  perSystem = { pkgs, lib, system, ... }:
  let
    my_jdk = pkgs.jdk21_headless;
    plugin = pkgs.callPackage (import ./protoc-gen-grpc-java.nix) {};

    package = pkgs.maven.buildMavenPackage rec {
      pname = "morrigan";
      version = "1";

      src = ./..;
      mvnHash = "sha256-vlO8iwqvCsluESgaBZqKo6OPsJ5cgANPLXI9YLuTH4U=";

      mvnJdk = my_jdk;
      mvnParameters = "-P offline";
      buildOffline = true;
      doCheck = false;

      nativeBuildInputs = [ pkgs.makeWrapper pkgs.protobuf plugin ];

      installPhase = ''
        mkdir -p $out/bin $out/share/${pname}
        install -Tm644 \
          target/${pname}-1-SNAPSHOT-jar-with-dependencies.jar \
          $out/share/${pname}/${pname}.jar
        install -m755 mnctl $out/bin/

        makeWrapper ${lib.getExe my_jdk} $out/bin/${pname} \
          --add-flags "\
            -XX:+PerfDisableSharedMem \
            -XX:-UsePerfData \
            -Xmx1024m \
            -Djava.net.preferIPv4Stack=true \
            -Djna.library.path='${pkgs.libvlc}' \
            -jar $out/share/${pname}/${pname}.jar \
          "
      '';

      meta = with lib; {
        description = "Morrigan media player";
        homepage = "https://github.com/haku/morrigan";
        license = licenses.asl20;
        mainProgram = pname;
      };
    };
  in {
    packages = {
      morrigan = package;
    };
    make-shells.default = {
      packages = [
        my_jdk
        plugin
      ];
    };
  };
}
