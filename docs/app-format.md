# App file format (v1)

The editor writes one JSON file per app into `apps/`. You can also write it by hand.

```json
{
  "name": "Tally",
  "id": "tally",
  "color": "#F2C46D",
  "blocks": [
    { "type": "title", "text": "Tally" },
    { "type": "counter", "label": "Count", "goal": 33, "step": 1, "gesture": true },
    { "type": "text", "text": "Double tap or double pinch to count" }
  ]
}
```

| Field | Rules |
|---|---|
| `name` | 1 to 24 letters, digits, spaces or `. ' & ! ? -`. Shown under the app icon. |
| `id` | 2 to 31 lowercase letters or digits, starting with a letter. Must match the file name (`apps/<id>.json`). |
| `color` | Hex colour like `#F2C46D`. |
| `blocks` | 1 to 8 blocks, drawn top to bottom. |

| Block | Fields |
|---|---|
| `title` | `text` (1 to 40 characters) |
| `text` | `text` (1 to 40 characters) |
| `counter` | `label`, `goal` (0 = no goal, up to 100000), `step` (1 to 1000), `gesture` |
| `timer` | `label`, `minutes` (1 to 180), `gesture` |

`gesture: true` maps the watch's hands-free gesture (Apple Watch double tap on watchOS 11+, Pixel Watch double pinch on Wear OS 7) to that block's main button. Only one block per app can use it.

Anything else is rejected by `tools/validate.py` before a build starts, because app files can come from anyone through pull requests.
