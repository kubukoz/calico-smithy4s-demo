{
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs = { nixpkgs, flake-utils, ... }: flake-utils.lib.eachDefaultSystem (system:
    let pkgs = import nixpkgs { inherit system; }; in
    {
      devShells.default = pkgs.mkShell {
        packages = [ pkgs.nodejs pkgs.yarn ];
        nativeBuildInputs = [ pkgs.s2n-tls ];
      };
    });
}
