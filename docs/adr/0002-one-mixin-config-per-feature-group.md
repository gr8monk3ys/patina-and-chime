# One mixin config per feature group

Each feature group (hopper, items, enchanting, sound) has its own `patinachime.<group>.mixins.json`, and client-only mixins go in a separate client config declared with `"environment": "client"` in `fabric.mod.json`. Features therefore never share a mixin config, so a failing mixin names its group and a feature can be removed by deleting its package and its config. The alternative, a single shared config, was rejected because one bad injection would fail every feature at once.
