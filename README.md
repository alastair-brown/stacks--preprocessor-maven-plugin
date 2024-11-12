### Stacks Preprocessor Maven Plugin

A custom Maven plugin that works alongside the Manifold Preprocessor https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor, to produce a Java Spring boot project based on options selected as Maven profiles. 

- Clean - Delete the Manifold pre processor output directory and contents. 
- Compile - Move source code from output directory to src/main/java directory structure and generate POM file including dependencies based on chosen Maven Profiles. 
- Test Compile - Move test code from output directory to src/test/java directory structure.
