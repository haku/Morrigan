{ withSystem, moduleWithSystem, inputs, ... }:
{
  flake.nixosModules.default = moduleWithSystem (perSystem@{ config, self', ... }:
  { pkgs, lib, config, ... }:
  with lib;
  let
    ffmpeg = pkgs.ffmpeg-headless.override {
      # https://github.com/NixOS/nixpkgs/blob/nixos-24.11/pkgs/development/libraries/ffmpeg/generic.nix
      withUnfree = true;
      withFdkAac = true;
      withGsm = true;
    };
    cfg = config.services.morrigan;
  in
  {
    options.services.morrigan = {
      enable = mkEnableOption "enable morrigan";

      groups = mkOption {
        description = "list of groups to attach via SupplementaryGroups";
        type = types.listOf types.str;
        default = [];
      };

      extraArgs = mkOption {
        type = with types; listOf str;
        default = [];
      };
    };

    config = mkIf cfg.enable {
      users.groups.morrigan = {};
      users.users.morrigan = {
        isSystemUser = true;
        group = "morrigan";
      };

      systemd.services.morrigan = {
        after = [ "network-online.target" ];
        requires = [ "network-online.target" ];
        wantedBy = [ "default.target" ];
        path = [ ffmpeg ];
        serviceConfig = {
          ExecStart = lib.escapeShellArgs([
              "${getExe perSystem.self'.packages.morrigan}"
          ] ++ cfg.extraArgs);

          User = "morrigan";
          Group = "morrigan";
          SupplementaryGroups = [ "audio" ] ++ cfg.groups;
          StateDirectory = "morrigan";

          Restart = "always";
          RestartSec = "60";
          KillSignal = "SIGINT";
          TimeoutStopSec = "30";

          AmbientCapabilities = "";
          CapabilityBoundingSet = "";
          DeviceAllow = "char-alsa";
          DevicePolicy = "closed";
          LockPersonality = true;
          MemoryDenyWriteExecute = false; # does not work with JVM
          NoNewPrivileges = true;
          PrivateTmp = true;
          ProcSubset = "pid";
          ProtectControlGroups = true;
          ProtectHome = true;
          ProtectKernelModules = true;
          ProtectKernelTunables = true;
          ProtectProc = "noaccess";
          ProtectSystem = "strict";
          RestrictAddressFamilies = "AF_INET AF_INET6 AF_NETLINK";
          RestrictNamespaces = true;
          RestrictRealtime = true;
          RestrictSUIDSGID = true;
          SystemCallArchitectures = "native";
        };
      };
    };

    # TODO setup nginx (if enabled by an option).

  }
  );
}
