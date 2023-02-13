# Important versioning information

Versions for `cljest` and `jest-preset-cljest` are done using a modified [semver](https://semver.org) configuration. The versioning system is roughly analogous to standard semver, but there are some differences:

Long story short:

- Patch versions of the same minor version will work together.
- Differing minor versions may not work together but upgrading will be easy.
- Different major versions probably won't work together and you might need to do a little work to upgrade.

Long story long:

- Patch versions are guaranteed to be compatible. `cljest` *1.0.0* and `jest-preset-cljest` *1.0.7* will work together.
- Minor versions *may* work together, but it's not guaranteed. This means that *1.1.0* and *1.3.0* might not work together. However, upgrading from *1.1.0* to *1.3.0* is guaranteed to require little/no migration for the developer, and instead may change the semantics of how the two libraries interface from a non-developer facing perspective.
- Major versions must be kept in sync and differing versions will both not work together and may require some work for the developer. Any changes necessary will be accompanied by a upgrade guide.

To avoid issues related to versions, it's best to keep the versions of both in sync. There will always be a corresponding version of every major, minor, and patch version of the libraries, even if no changes have been made in one of them, so that it's possible to always use the same version. Also note that prerelease versions of one library may not have a corresponding version in the other -- `cljest` *1.7.0-alpha1* may not have a corresponding `jest-preset-cljest` *1.7.0-alpha1*.
