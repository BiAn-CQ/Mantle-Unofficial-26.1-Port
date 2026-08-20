# Changelog

## 26.1.2-1.12.0

- Port Mantle to Minecraft 26.1.2 and NeoForge 26.1.2.95.
- Add the NeoForge 26.1 resource-handler compatibility bridge used by the
  Tinkers' Construct 26.1 port.
- Restore JUnit coverage for typed maps, primitive/collection/record loadables,
  and all bundled JSON resources.
- Fix `TypedMap.EMPTY#getOrDefault` so it returns the caller's default value.
- Reject empty color strings with a JSON syntax error instead of an index error.
- Update JEI integration to the current 29.x ingredient builder API.
- Add isolated GameTest and dedicated-server release-smoke run configurations.
