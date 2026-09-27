---
name: spline-rive-interactive-runtime
description: Expert guide for integrating and controlling interactive Spline 3D and Rive 2D state machine runtime animations in React and modern web apps. Covers @splinetool/react-spline, runtime event listeners, variable manipulation, object targeting, @rive-app/react-canvas, StateMachine inputs (Boolean, Trigger, Number), layout fits, and performance optimization.
---

# Spline & Rive Interactive Runtime

## When to Use This Skill

Use this skill when:
- Embedding interactive 3D assets designed in **Spline** into React, Next.js, or HTML pages.
- Interacting with Spline scenes programmatically (triggering animations, updating variables, listening to clicks/hovers, finding objects by name/ID).
- Embedding lightweight, 60fps vector animations designed in **Rive** into web applications.
- Controlling Rive state machines via code inputs (`Boolean`, `Trigger`, `Number`), driving animations with user interactions, scroll, or form states.
- Choosing between 3D spatial scenes (Spline) and vector state-machine UI micro-interactions (Rive).

---

## Spline vs. Rive: Decision Matrix

| Feature | Spline (`@splinetool/react-spline`) | Rive (`@rive-app/react-canvas`) |
|---------|--------------------------------------|-----------------------------------|
| **Dimension** | Full 3D (Polygons, lights, cameras, physics) | 2D / 2.5D Vector graphics & meshes |
| **Runtime Size** | ~1.5MB – 3MB (WebGL runtime) | ~150KB – 300KB (WASM runtime) |
| **Frame Rates** | 30 – 60 FPS (GPU-dependent) | 60 – 120 FPS (Extremely lightweight) |
| **Best For** | 3D hero sections, interactive 3D product cards, spatial rooms | Stateful icons, dynamic buttons, mascots, complex loading spinners, UI micro-interactions |

---

## Part 1: Spline Interactive Runtime

### Installation
```bash
npm install @splinetool/react-spline @splinetool/runtime
```

### 1. Basic React Component with Loading Fallback
```jsx
import React, { useState } from 'react';
import Spline from '@splinetool/react-spline';

export function SplineHero() {
  const [isLoading, setIsLoading] = useState(true);

  return (
    <div className="relative w-full h-[600px] overflow-hidden rounded-2xl bg-neutral-900">
      {isLoading && (
        <div className="absolute inset-0 flex items-center justify-center text-neutral-400">
          Loading 3D experience...
        </div>
      )}
      <Spline
        scene="https://prod.spline.design/your-scene-id/scene.splinecode"
        onLoad={() => setIsLoading(false)}
      />
    </div>
  );
}
```

### 2. Programmatic Control & Object Manipulation
Target objects within the scene, listen to spline events, and modify variables:

```jsx
import React, { useRef } from 'react';
import Spline from '@splinetool/react-spline';

export function InteractiveProduct() {
  const splineRef = useRef();

  function onLoad(splineApp) {
    splineRef.current = splineApp;

    // 1. Find 3D object by name (assigned in Spline Editor)
    const cube = splineApp.findObjectByName('InteractiveCube');
    if (cube) {
      cube.scale.x = 1.2;
    }
  }

  function handleTriggerEvent() {
    if (!splineRef.current) return;

    // 2. Emit event configured in Spline
    splineRef.current.emitEvent('mouseHover', 'InteractiveCube');

    // 3. Update Spline variables (if configured)
    splineRef.current.setVariable('ColorState', 2);
  }

  return (
    <div>
      <Spline
        scene="https://prod.spline.design/your-scene-id/scene.splinecode"
        onLoad={onLoad}
        onSplineMouseDown={(e) => console.log('Clicked 3D object:', e.target.name)}
      />
      <button onClick={handleTriggerEvent} className="mt-4 px-4 py-2 bg-indigo-600 text-white rounded">
        Trigger 3D Action
      </button>
    </div>
  );
}
```

---

## Part 2: Rive Interactive Runtime

### Installation
```bash
npm install @rive-app/react-canvas
```

### 1. State Machine Controller Component
Control state machine inputs (`Boolean`, `Trigger`, `Number`) to react dynamically to application state:

```jsx
import React from 'react';
import { useRive, useStateMachineInput, Layout, Fit, Alignment } from '@rive-app/react-canvas';

export function RiveLikeButton({ isLiked, onToggle }) {
  const STATE_MACHINE = 'LikeButtonStateMachine';

  const { rive, RiveComponent } = useRive({
    src: '/animations/like_button.riv',
    stateMachines: STATE_MACHINE,
    autoplay: true,
    layout: new Layout({
      fit: Fit.Contain,
      alignment: Alignment.Center,
    }),
  });

  // Access State Machine Inputs
  const likedInput = useStateMachineInput(rive, STATE_MACHINE, 'isLiked', false);
  const clickTrigger = useStateMachineInput(rive, STATE_MACHINE, 'onClickTrigger');

  function handleClick() {
    if (clickTrigger) clickTrigger.fire();
    if (likedInput) likedInput.value = !likedInput.value;
    onToggle?.();
  }

  return (
    <button 
      onClick={handleClick}
      className="w-16 h-16 relative focus:outline-none"
      aria-label="Like"
    >
      <RiveComponent />
    </button>
  );
}
```

### 2. Cursor Follow / Number Input Driving
Drive a mascot or character's gaze angle using pointer coordinates:

```jsx
import React from 'react';
import { useRive, useStateMachineInput } from '@rive-app/react-canvas';

export function GazeMascot() {
  const STATE_MACHINE = 'GazeController';

  const { rive, RiveComponent } = useRive({
    src: '/animations/mascot.riv',
    stateMachines: STATE_MACHINE,
    autoplay: true,
  });

  const lookX = useStateMachineInput(rive, STATE_MACHINE, 'lookX', 50); // 0 to 100
  const lookY = useStateMachineInput(rive, STATE_MACHINE, 'lookY', 50);

  function handleMouseMove(e) {
    if (!lookX || !lookY) return;
    const { clientX, clientY } = e;
    const xPercent = (clientX / window.innerWidth) * 100;
    const yPercent = (clientY / window.innerHeight) * 100;

    lookX.value = Math.max(0, Math.min(100, xPercent));
    lookY.value = Math.max(0, Math.min(100, yPercent));
  }

  return (
    <div onMouseMove={handleMouseMove} className="w-full h-96">
      <RiveComponent />
    </div>
  );
}
```

---

## Critical Best Practices

1. **Page Scroll Trapping**:
   - Spline embeds intercept wheel events by default. Set `pointer-events-none` on the canvas wrapper or disable zoom inside Spline's camera settings unless full interactive 3D inspection is required.
2. **Resource Cleanup**:
   - Ensure Rive instances unmount cleanly to free WASM memory buffers. `useRive` handles this automatically in React.
3. **Viewport Caching**:
   - Pause Spline and Rive playback when scrolled off-screen (`IntersectionObserver`) to preserve battery and GPU cycles on mobile devices.

---

## Reference Documents

- [Spline Interactive Guide](file:///c:/Users/Lenovo/OneDrive/Desktop/AntiGravity/.agent/skills/spline-rive-interactive-runtime/references/spline-interactive.md)
- [Spline API Reference](file:///c:/Users/Lenovo/OneDrive/Desktop/AntiGravity/.agent/skills/spline-rive-interactive-runtime/references/spline-api-reference.md)
- [Rive Interactive Guide](file:///c:/Users/Lenovo/OneDrive/Desktop/AntiGravity/.agent/skills/spline-rive-interactive-runtime/references/rive-interactive.md)
- [Rive API Reference](file:///c:/Users/Lenovo/OneDrive/Desktop/AntiGravity/.agent/skills/spline-rive-interactive-runtime/references/rive-api-reference.md)
