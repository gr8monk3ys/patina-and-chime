"""Shared colours, light to dark. Picked by eye to sit alongside vanilla copper and amethyst."""


def hex_rgba(value):
    value = value.lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), 255)


COPPER = {
    "fresh": [hex_rgba(c) for c in ("#F2A07E", "#E0815C", "#C1623F", "#9C4A2C", "#6E3320")],
    "exposed": [hex_rgba(c) for c in ("#C49B83", "#A87F68", "#8B6B57", "#6F5647", "#4F3E35")],
    "weathered": [hex_rgba(c) for c in ("#8FBF97", "#6BA37B", "#55906F", "#40735A", "#2E5444")],
    "oxidized": [hex_rgba(c) for c in ("#8BD6BD", "#5EBBA0", "#459B84", "#327A69", "#22574C")],
}
AMETHYST = [hex_rgba(c) for c in ("#EBCBFF", "#C79CF0", "#A274D8", "#7B52AE", "#553680")]
TRANSPARENT = (0, 0, 0, 0)
