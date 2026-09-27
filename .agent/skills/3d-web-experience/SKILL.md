---
name: 3d-web-experience
description: Expert in building 3D experiences for the web - Three.js, React Three Fiber, Spline, WebGL, and interactive 3D scenes. Covers product configurators, 3D portfolios, immersive websites, and bringing depth to web experiences. Use when 3D website, three.js, WebGL, react three fiber, 3D experience, spline, product configurator are mentioned.
---

# 3D Web Experience

## Identity & Role

**Role**: 3D Web Experience Architect  
**Mission**: Bring depth, interactivity, and delight to the web while fiercely guarding performance, mobile usability, and load times.

You bring the third dimension to the web. You know when 3D enhances a project and when it is merely a distraction. You balance visual fidelity with strict frame-rate targets and memory budgets. You ensure 3D is accessible, graceful on low-end devices, and intuitive for users who have never interacted with a 3D interface before.

### Core Expertise
- **Frameworks & Libraries**: Three.js, React Three Fiber (R3F), `@react-three/drei`, Spline (`@splinetool/react-spline`), Babylon.js.
- **Rendering & Shaders**: WebGL, GLSL custom shaders, post-processing (`@react-three/postprocessing`).
- **Asset Pipeline**: 3D model compression (`gltf-transform`, Draco, KTX2/WebP textures), Blender baking, LOD (Level of Detail).
- **Interactions**: Scroll-driven 3D (GSAP ScrollTrigger, R3F ScrollControls), Raycasting, OrbitControls, product customization.
- **Performance Engineering**: Draw call batching, instanced meshes, GPU memory lifecycle management, mobile DPR clamping.

---

## 3D Stack Selection

Choose the technology stack that fits the project scope, timeline, and tech stack:

### Comparison Matrix
| Tool | Best For | Learning Curve | Control | File Overhead |
|------|----------|----------------|---------|---------------|
| **Spline** | Fast prototypes, designer handoff, landing page embeds | Low | Medium | High (runtime) |
| **React Three Fiber (R3F)** | React/Next.js apps, complex interactive scenes, configurators | Medium | High | Modular |
| **Vanilla Three.js** | Non-framework web apps, standalone canvas, maximum custom control | High | Maximum | Minimal |
| **Babylon.js** | Complex browser games, advanced physics-heavy simulations | High | Maximum | Large |

### Decision Flow
```
Need quick embed or designer-authored interactive model?
├── Yes ──> Spline
└── No  ──> Using React / Next.js?
            ├── Yes ──> React Three Fiber + Drei
            └── No  ──> Vanilla Three.js
```

---

## Standard Implementations

### 1. Spline Embed (Fastest Setup)
```jsx
import Spline from '@splinetool/react-spline';

export default function HeroScene() {
  return (
    <div className="w-full h-[600px] relative">
      <Spline scene="https://prod.spline.design/your-scene-id/scene.splinecode" />
    </div>
  );
}
```

### 2. React Three Fiber (R3F) Production Scene
```jsx
import React, { Suspense } from 'react';
import { Canvas } from '@react-three/fiber';
import { OrbitControls, useGLTF, useProgress, Html } from '@react-three/drei';

function Loader() {
  const { progress } = useProgress();
  return (
    <Html center className="text-white text-sm font-sans tracking-wide">
      {progress.toFixed(0)}% loaded
    </Html>
  );
}

function ProductModel({ url, color }) {
  const { scene } = useGLTF(url);
  return <primitive object={scene} />;
}

export default function ThreeScene() {
  const isMobile = typeof window !== 'undefined' && window.innerWidth < 768;

  return (
    <div className="w-full h-screen bg-neutral-950">
      <Canvas
        camera={{ position: [0, 1.5, 4], fov: 45 }}
        dpr={isMobile ? 1 : [1, 2]} // Clamp DPR on mobile
        performance={{ min: 0.5 }}
      >
        <ambientLight intensity={0.6} />
        <directionalLight position={[5, 8, 5]} intensity={1.2} castShadow />
        
        <Suspense fallback={<Loader />}>
          <ProductModel url="/models/product.glb" />
          <OrbitControls 
            enableZoom={false} // Prevent intercepting page scroll
            enablePan={false}
            maxPolarAngle={Math.PI / 2} 
          />
        </Suspense>
      </Canvas>
    </div>
  );
}
```

---

## 3D Asset Pipeline & Optimization

### Target Budgets
| Asset Type | Max File Size | Target Polycount | Max Texture Res |
|------------|---------------|------------------|-----------------|
| Hero Model | 2 – 5 MB | < 100K tris | 2048 x 2048 |
| Product / Item | 1 – 2 MB | < 50K tris | 1024 x 1024 |
| Background Prop | < 500 KB | < 10K tris | 512 x 512 |
| Total Mobile Scene | < 5 MB | < 80K tris total | 1024 max |

### GLTF Compression Workflow
Use `gltf-transform` to apply Draco geometry compression and WebP/KTX2 texture compression:
```bash
# Optimize and compress model
npx @gltf-transform/cli optimize input.glb output.glb \
  --compress draco \
  --texture-resize 1024 \
  --texture-compress webp
```

---

## Scroll-Driven 3D Animations

### R3F with ScrollControls
```jsx
import { Canvas, useFrame } from '@react-three/fiber';
import { ScrollControls, useScroll } from '@react-three/drei';
import { useRef } from 'react';

function RotatingObject() {
  const meshRef = useRef();
  const scroll = useScroll();

  useFrame(() => {
    // scroll.offset is 0 to 1
    meshRef.current.rotation.y = scroll.offset * Math.PI * 2;
    meshRef.current.position.z = -scroll.offset * 2;
  });

  return (
    <mesh ref={meshRef}>
      <boxGeometry args={[1, 1, 1]} />
      <meshStandardMaterial color="#4f46e5" />
    </mesh>
  );
}

export default function ScrollScene() {
  return (
    <Canvas>
      <ScrollControls pages={3} damping={0.2}>
        <RotatingObject />
      </ScrollControls>
    </Canvas>
  );
}
```

### GSAP ScrollTrigger Integration
```javascript
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

export function setupScrollAnimation(camera, mesh) {
  gsap.to(camera.position, {
    scrollTrigger: {
      trigger: '#scene-container',
      start: 'top top',
      end: 'bottom bottom',
      scrub: 1,
    },
    x: 2,
    y: 1,
    z: 5,
  });
}
```

---

## Sharp Edges & Critical Gotchas

1. **WebGL Context Loss (`webglcontextlost`)**:
   - *Cause*: Exceeding GPU memory, creating multiple `Canvas` instances, or mobile tab switches.
   - *Fix*: Keep only 1 WebGL context per page. Always dispose geometries, materials, and textures when unmounting:
     ```jsx
     useEffect(() => {
       return () => {
         scene.traverse((obj) => {
           if (obj.geometry) obj.geometry.dispose();
           if (obj.material) {
             [].concat(obj.material).forEach(m => m.dispose());
           }
         });
       };
     }, []);
     ```

2. **OrbitControls Page Scroll Interception**:
   - *Cause*: Canvas consumes touch and mousewheel events, locking users from scrolling down the page.
   - *Fix*: Set `enableZoom={false}` on `OrbitControls` if the scene is inside a vertically scrolling page, or toggle controls only on explicit hover/click.

3. **Shader Crashes on Mobile**:
   - *Cause*: Missing precision declarations or hardware differences in GLSL fragment shaders.
   - *Fix*: Always specify precision:
     ```glsl
     precision mediump float;
     ```

4. **Missing Fallbacks**:
   - *Cause*: Low-end devices or locked-down browsers without WebGL fail silently or show a blank canvas.
   - *Fix*: Detect WebGL capability and supply a high-fidelity WebP render as fallback.

---

## Quality & Validation Checklist

- [ ] **Loading Feedback**: Scene includes an active loader (`useProgress` or spinner fallback) to prevent blank canvas bounce.
- [ ] **Mobile Performance**: DPR is capped at 1 on mobile screens (`dpr={isMobile ? 1 : 2}`).
- [ ] **Asset Size**: Models are compressed with Draco/WebP and under 5MB.
- [ ] **Scroll Ergonomics**: Canvas does not hijack normal page scroll gestures.
- [ ] **WebGL Support Fallback**: Non-WebGL browsers receive a styled screenshot or fallback card.

---

## Detailed References

For full specifications and code samples, consult:
- **`references/patterns.md`**: Architectural patterns for stack selection, model pipelines, scroll 3D, and optimization.
- **`references/sharp_edges.md`**: In-depth explanations of bugs, context lost recovery, memory limits, and shader issues.
- **`references/validations.md`**: Strict validation rules and check criteria for 3D web applications.
