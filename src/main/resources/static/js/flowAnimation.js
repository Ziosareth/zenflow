// Dot Matrix Animation - Animated background with dot matrix pattern
// This script creates an animated dot matrix background

document.addEventListener('DOMContentLoaded', function() {
    // Create canvas element for the animation
    const canvas = document.createElement('canvas');
    canvas.id = 'flowCanvas';

    // Style the canvas to cover the entire viewport
    Object.assign(canvas.style, {
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100%',
        height: '100%',
        zIndex: -1, // Place behind content
        pointerEvents: 'none' // Allow interaction with elements below
    });

    // Add canvas to the body
    document.body.prepend(canvas);

    // Initialize the animation
    const dotMatrixAnimation = new DotMatrixAnimation(canvas);
    dotMatrixAnimation.start();
});

class DotMatrixAnimation {
    constructor(canvas) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');

        // Animation settings
        this.dotSize = 2; // Size of each dot
        this.spacing = 20; // Space between dots
        this.baseHue = 210; // Base color (blue)
        this.hueRange = 30; // Color variation range
        this.waveSpeed = 0.02; // Speed of the wave animation
        this.waveAmplitude = 0.3; // Intensity of the wave effect

        // Animation state
        this.time = 0;
        this.dots = [];
        this.columns = 0;
        this.rows = 0;

        // Bind event handlers
        this.handleResize = this.handleResize.bind(this);
        this.animate = this.animate.bind(this);

        // Set up event listeners
        window.addEventListener('resize', this.handleResize);

        // Initial setup
        this.handleResize();
        this.initDotMatrix();
    }

    handleResize() {
        // Set canvas dimensions to match window size
        this.canvas.width = window.innerWidth;
        this.canvas.height = window.innerHeight;

        // Re-initialize dot matrix when resizing
        this.initDotMatrix();
    }

    initDotMatrix() {
        this.dots = [];

        // Calculate number of columns and rows based on spacing
        this.columns = Math.ceil(this.canvas.width / this.spacing) + 1;
        this.rows = Math.ceil(this.canvas.height / this.spacing) + 1;

        // Create dot matrix
        for (let y = 0; y < this.rows; y++) {
            for (let x = 0; x < this.columns; x++) {
                this.dots.push({
                    x: x * this.spacing,
                    y: y * this.spacing,
                    baseSize: this.dotSize,
                    size: this.dotSize,
                    hue: this.baseHue + Math.random() * this.hueRange,
                    opacity: 0.1 + Math.random() * 0.5
                });
            }
        }
    }

    start() {
        // Start animation loop
        this.animate();
    }

    animate() {
        // Clear canvas
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        // Update animation time
        this.time += this.waveSpeed;

        // Draw background gradient
        this.drawBackground();

        // Update and draw dots
        this.updateDots();
        this.drawDots();

        // Continue animation loop
        requestAnimationFrame(this.animate);
    }

    drawBackground() {
        // Create a subtle gradient background
        const gradient = this.ctx.createLinearGradient(0, 0, this.canvas.width, this.canvas.height);
        gradient.addColorStop(0, 'rgba(10, 20, 40, 0.2)');
        gradient.addColorStop(1, 'rgba(5, 10, 30, 0.1)');

        this.ctx.fillStyle = gradient;
        this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
    }

    updateDots() {
        const centerX = this.canvas.width / 2;
        const centerY = this.canvas.height / 2;

        this.dots.forEach(dot => {
            // Calculate distance from center for wave effect
            const dx = dot.x - centerX;
            const dy = dot.y - centerY;
            const distance = Math.sqrt(dx * dx + dy * dy);

            // Create wave effect based on distance and time
            const wave = Math.sin(distance * 0.01 - this.time) * this.waveAmplitude;

            // Update dot size based on wave
            dot.size = dot.baseSize * (1 + wave);

            // Update dot opacity based on wave
            dot.opacity = 0.2 + Math.abs(wave) * 0.8;
        });
    }

    drawDots() {
        this.dots.forEach(dot => {
            // Draw dot
            this.ctx.beginPath();
            this.ctx.arc(dot.x, dot.y, dot.size, 0, Math.PI * 2);

            // Use HSL color for easy hue manipulation
            const hue = dot.hue;
            const saturation = '80%';
            const lightness = '65%';
            this.ctx.fillStyle = `hsla(${hue}, ${saturation}, ${lightness}, ${dot.opacity})`;

            this.ctx.fill();

            // Add subtle glow effect for brighter dots
            if (dot.opacity > 0.5) {
                const glowSize = dot.size * 2;
                const gradient = this.ctx.createRadialGradient(
                    dot.x, dot.y, 0,
                    dot.x, dot.y, glowSize
                );

                gradient.addColorStop(0, `hsla(${hue}, 90%, 70%, 0.2)`);
                gradient.addColorStop(1, `hsla(${hue}, 80%, 60%, 0)`);

                this.ctx.fillStyle = gradient;
                this.ctx.beginPath();
                this.ctx.arc(dot.x, dot.y, glowSize, 0, Math.PI * 2);
                this.ctx.fill();
            }
        });
    }
}
