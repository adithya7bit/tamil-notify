# 3D Web Experience - Patterns

## 1. 3D Stack Selection

### Options Comparison
| Tool | Best For | Learning Curve | Control |
|------|----------|----------------|---------|
| **Spline** | Quick prototypes, designers, simple landing page props | Low | Medium |
| **React Three Fiber (R3F)** | React apps, interactive 3D, configurators | Medium | High |
| **Three.js Vanilla** | Non-framework apps, standalone canvas, maximum custom control | High | Maximum |
| **Babylon.js** | Complex 3D browser games, heavy physics | High | Maximum |

### Decision Tree
```
Need quick 3D element or designer-made scene?
└── Yes → Spline
└── No → Continue

Using React?
└── Yes → React Three Fiber
└── No → Continue

Need maximum performance / lowest abstraction?
└── Yes → Vanilla Three.js
└── No → Spline or R3F
```

### Spline Integration
```jsx
import Spline from '@splinetool/react-spline';

export default function Scene() {
  return (
    <Spline scene="https://prod.spline.design/your-scene-hash/scene.splinecode" />
  );
}
```

### React Three Fiber Setup
```jsx
import { Canvas } from '@react-three/fiber';
import { OrbitControls, useGLTF } from '@react-three/drei';

function Model() {
  const { scene } = useGLTF('/model.glb');
  return <primitive object={scene} />;
}

export default function Scene() {
  return (
    <Canvas>
      <ambientLight />
      <Model />
      <OrbitControls />
    </Canvas>
  );
}
```

---

## 2. 3D Model Pipeline

### Format Selection
| Format | Use Case | Size / Efficiency |
|--------|----------|-------------------|
| **GLB/GLTF** | Standard web 3D (Standard) | Smallest, web optimized |
| **FBX** | DCC interchange format | Large (convert before web use) |
| **OBJ** | Simple static meshes | Medium, lacks PBR |
| **USDZ** | Apple AR Quick Look | Native on iOS/macOS |

### Optimization Pipeline
1. Model in Blender or CAD tool.
2. Reduce triangle count (< 100K for hero, < 30K for mobile).
3. Combine materials and bake textures (PBR: BaseColor, Roughness/Metalness, Normal).
4. Export as `.glb`.
5. Compress with `gltf-transform`.
6. Verify file size is under 5MB (under 2MB preferred).

### Compression Command
```bash
# Using gltf-transform
npx @gltf-transform/cli optimize input.glb output.glb \
  --compress draco \
  --texture-resize 1024 \
  --texture-compress webp
```

### Loading in R3F with Progress Feedback
```jsx
import { useGLTF, useProgress, Html } from '@react-three/drei';
import { Suspense } from 'react';

function Loader() {
  const { progress } = useProgress();
  return <Html center>{progress.toFixed(0)}%</Html>;
}

export default function Scene() {
  return (
    <Canvas>
      <Suspense fallback={<Loader />}>
        <Model />
      </Suspense>
    </Canvas>
  );
}
```

---

## 3. Scroll-Driven 3D

### R3F + ScrollControls
```jsx
import { ScrollControls, useScroll } from '@react-three/drei';
import { useFrame } from '@react-three/fiber';
import { useRef } from 'react';

function RotatingModel() {
  const scroll = useScroll();
  const ref = useRef();

  useFrame(() => {
    // Rotate based on scroll progress (0.0 to 1.0)
    ref.current.rotation.y = scroll.offset * Math.PI * 2;
  });

  return <mesh ref={ref}>...</mesh>;
}

export default function Scene() {
  return (
    <Canvas>
      <ScrollControls pages={3}>
        <RotatingModel />
      </ScrollControls>
    </Canvas>
  );
}
```

### GSAP + Three.js ScrollTrigger
```javascript
import gsap from 'gsap';
import ScrollTrigger from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

gsap.to(camera.position, {
  scrollTrigger: {
    trigger: '.scene-section',
    scrub: true,
  },
  z: 5,
  y: 2,
});
```

---

## 4. Performance Optimization

### Target Framerate & Budget
| Device Tier | Target FPS | Max Triangles | Max Draw Calls |
|-------------|------------|---------------|----------------|
| High-end Desktop | 60 FPS | 500K | < 100 |
| Standard Mobile | 30 – 60 FPS | 80K | < 40 |
| Low-end Device | 30 FPS | 30K | < 20 |

### Quick Wins
1. **Instanced Meshes**: Render repeated objects using `InstancedMesh` or `<Instances>` from Drei.
2. **Limit Active Lights**: Prefer 1 ambient light + 1 directional light. Avoid heavy shadow maps where unneeded.
3. **Clamp Device Pixel Ratio (DPR)**: Never use `window.devicePixelRatio` unrestricted on high-DPI retina mobile screens.
4. **Lazy Load Models**: Keep the 3D canvas unmounted until the section approaches viewport.

```jsx
const isMobile = /iPhone|iPad|Android/i.test(navigator.userAgent);

<Canvas
  dpr={isMobile ? 1 : [1, 1.5]}
  performance={{ min: 0.5 }}
>
  ...
</Canvas>
```
