/**
 * CheerpJ 4.2 Loader - Local Version
 * 
 * This is a placeholder file that should be replaced with the actual CheerpJ loader.js
 * from https://cjrtnc.leaningtech.com/4.2/loader.js?v=20250804
 * 
 * To properly implement this:
 * 1. Download the actual loader.js file from the URL above
 * 2. Replace this file with the downloaded content
 * 
 * This placeholder provides the expected function signatures for development/testing
 * but the actual CheerpJ implementation is required for functionality.
 */

// NOTE: This is a development placeholder - replace with actual CheerpJ loader.js content

console.warn('PLACEHOLDER: Using mock CheerpJ loader. Replace with actual loader.js for production use.');

// Mock implementations for development
window.cheerpjInit = function(config) {
    console.log('Mock cheerpjInit called with config:', config);
    return Promise.resolve();
};

window.cheerpOSCreateDir = function(path, options, mode, callback) {
    console.log('Mock cheerpOSCreateDir called:', path);
    if (callback) callback();
};

window.cheerpjWriteFile = function(path, data, callback) {
    console.log('Mock cheerpjWriteFile called:', path, 'size:', data.length);
    if (callback) callback(null);
};

window.cheerpOSOpen = function(fds, path, mode, callback) {
    console.log('Mock cheerpOSOpen called:', path, mode);
    if (callback) callback(1); // Return mock file descriptor
};

window.cheerpOSWrite = function(fd, data, offset, length, callback) {
    console.log('Mock cheerpOSWrite called, fd:', fd, 'length:', length);
    if (callback) callback(length);
};

window.cheerpOSClose = function(fd, callback) {
    console.log('Mock cheerpOSClose called, fd:', fd);
    if (callback) callback();
};

window.cheerpjCreateDisplay = function(width, height) {
    console.log('Mock cheerpjCreateDisplay called:', width, 'x', height);
};

window.cheerpjRunMain = function(className, args) {
    console.log('Mock cheerpjRunMain called:', className, args);
    console.error('Cannot run Java application - this is a mock CheerpJ implementation');
    return Promise.reject(new Error('Mock implementation - actual CheerpJ loader required'));
};

console.log('Mock CheerpJ loader initialized. Functions available:', 
    Object.keys(window).filter(key => key.startsWith('cheerpj') || key.startsWith('cheerpOS')));