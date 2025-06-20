// Initialize theme based on user preference
document.addEventListener('DOMContentLoaded', function() {
    const savedTheme = localStorage.getItem('theme');
    
    if (savedTheme) {
        const body = document.body;
        
        // Remove existing theme classes
        body.classList.remove('theme-light', 'theme-dark');
        
        if (savedTheme === 'light') {
            body.classList.add('theme-light');
        } else if (savedTheme === 'dark') {
            body.classList.add('theme-dark');
        }
        // If 'system', don't add any class
    }
});