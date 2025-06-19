// Flow Animation - Cursor-reactive background animation
// This script creates animated flows that react to cursor movements

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
    const flowAnimation = new FlowAnimation(canvas);
    flowAnimation.start();
});

class FlowAnimation {
    constructor(canvas) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');
        
        // Track mouse/touch position
        this.mouseX = 0;
        this.mouseY = 0;
        
        // Particles array
        this.particles = [];
        
        // Animation settings
        this.particleCount = 50;
        this.baseHue = 220; // Blue base color
        this.hueRange = 40;
        this.baseSpeed = 0.5;
        this.speedRange = 1;
        this.baseSizeRange = [1, 3];
        
        // Bind event handlers
        this.handleResize = this.handleResize.bind(this);
        this.handleMouseMove = this.handleMouseMove.bind(this);
        this.handleTouchMove = this.handleTouchMove.bind(this);
        this.animate = this.animate.bind(this);
        
        // Set up event listeners
        window.addEventListener('resize', this.handleResize);
        window.addEventListener('mousemove', this.handleMouseMove);
        window.addEventListener('touchmove', this.handleTouchMove, { passive: true });
        
        // Initial setup
        this.handleResize();
        this.initParticles();
    }
    
    handleResize() {
        // Set canvas dimensions to match window size
        this.canvas.width = window.innerWidth;
        this.canvas.height = window.innerHeight;
        
        // Re-initialize particles when resizing
        this.initParticles();
    }
    
    handleMouseMove(e) {
        this.mouseX = e.clientX;
        this.mouseY = e.clientY;
    }
    
    handleTouchMove(e) {
        if (e.touches.length > 0) {
            this.mouseX = e.touches[0].clientX;
            this.mouseY = e.touches[0].clientY;
        }
    }
    
    initParticles() {
        this.particles = [];
        
        // Create particles
        for (let i = 0; i < this.particleCount; i++) {
            this.particles.push({
                x: Math.random() * this.canvas.width,
                y: Math.random() * this.canvas.height,
                size: this.baseSizeRange[0] + Math.random() * (this.baseSizeRange[1] - this.baseSizeRange[0]),
                speedX: (Math.random() - 0.5) * this.speedRange,
                speedY: (Math.random() - 0.5) * this.speedRange,
                hue: this.baseHue + Math.random() * this.hueRange
            });
        }
    }
    
    start() {
        // Start animation loop
        this.animate();
    }
    
    animate() {
        // Clear canvas
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        
        // Update and draw particles
        this.updateParticles();
        this.drawParticles();
        
        // Continue animation loop
        requestAnimationFrame(this.animate);
    }
    
    updateParticles() {
        const centerX = this.canvas.width / 2;
        const centerY = this.canvas.height / 2;
        
        // Calculate mouse influence
        const mouseInfluenceRadius = Math.max(this.canvas.width, this.canvas.height) * 0.3;
        
        this.particles.forEach(particle => {
            // Base movement
            particle.x += particle.speedX;
            particle.y += particle.speedY;
            
            // Mouse influence - particles are attracted to mouse position
            if (this.mouseX && this.mouseY) {
                const dx = this.mouseX - particle.x;
                const dy = this.mouseY - particle.y;
                const distance = Math.sqrt(dx * dx + dy * dy);
                
                if (distance < mouseInfluenceRadius) {
                    const influence = (1 - distance / mouseInfluenceRadius) * 0.05;
                    particle.speedX += dx * influence;
                    particle.speedY += dy * influence;
                }
            }
            
            // Gentle pull toward center to keep particles on screen
            const centerDx = centerX - particle.x;
            const centerDy = centerY - particle.y;
            const centerDistance = Math.sqrt(centerDx * centerDx + centerDy * centerDy);
            const centerInfluence = 0.0005;
            
            particle.speedX += centerDx * centerInfluence;
            particle.speedY += centerDy * centerInfluence;
            
            // Apply friction to prevent excessive speed
            particle.speedX *= 0.99;
            particle.speedY *= 0.99;
            
            // Wrap around edges
            if (particle.x < 0) particle.x = this.canvas.width;
            if (particle.x > this.canvas.width) particle.x = 0;
            if (particle.y < 0) particle.y = this.canvas.height;
            if (particle.y > this.canvas.height) particle.y = 0;
        });
    }
    
    drawParticles() {
        // Draw connections between nearby particles
        this.ctx.strokeStyle = 'rgba(200, 220, 255, 0.1)';
        this.ctx.lineWidth = 0.5;
        
        for (let i = 0; i < this.particles.length; i++) {
            const particleA = this.particles[i];
            
            for (let j = i + 1; j < this.particles.length; j++) {
                const particleB = this.particles[j];
                const dx = particleA.x - particleB.x;
                const dy = particleA.y - particleB.y;
                const distance = Math.sqrt(dx * dx + dy * dy);
                
                // Connect particles that are close to each other
                const maxDistance = 100;
                if (distance < maxDistance) {
                    this.ctx.beginPath();
                    this.ctx.moveTo(particleA.x, particleA.y);
                    this.ctx.lineTo(particleB.x, particleB.y);
                    
                    // Fade opacity based on distance
                    const opacity = 0.1 * (1 - distance / maxDistance);
                    this.ctx.strokeStyle = `rgba(200, 220, 255, ${opacity})`;
                    
                    this.ctx.stroke();
                }
            }
        }
        
        // Draw particles
        this.particles.forEach(particle => {
            this.ctx.beginPath();
            this.ctx.arc(particle.x, particle.y, particle.size, 0, Math.PI * 2);
            
            // Use HSL color for easy hue manipulation
            const hue = particle.hue;
            const saturation = '70%';
            const lightness = '70%';
            this.ctx.fillStyle = `hsla(${hue}, ${saturation}, ${lightness}, 0.8)`;
            
            this.ctx.fill();
        });
    }
}