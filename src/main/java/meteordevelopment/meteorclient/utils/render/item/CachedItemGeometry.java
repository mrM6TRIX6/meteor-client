package meteordevelopment.meteorclient.utils.render.item;

import java.util.List;

record CachedItemGeometry(List<CachedItemQuad> quads, boolean animated, boolean specialRenderer) { }
