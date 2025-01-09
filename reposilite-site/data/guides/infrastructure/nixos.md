---
id: nixos
title: NixOS
community: true
---

[nixpkgs](https://github.com/NixOS/nixpkgs) contains a package and NixOS module for Reposilite.

You can enable and configure it via `services.reposilite` in your NixOS configuration (e.g. `/etc/nixos/configuration.nix`):

```nix
{ config, pkgs, ... }:
{
  # ...

  services.reposilite = {
    enable = true;
    openFirewall = false;
    tokens = [
      "tokenName:tokenSecret" # It is recommended to use a secret management strategy such as sops-nix here.
    ];
    settings = { }; # Add settings that would usually go in the Reposilite config (`reposilite.cdn`) here, e.g. `sslEnabled = true`
  };
}
```
