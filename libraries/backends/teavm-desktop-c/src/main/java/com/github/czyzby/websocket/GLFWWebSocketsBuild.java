package com.github.czyzby.websocket;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/** Build-time helpers for TeaVM GLFW websocket native runtimes. */
public class GLFWWebSocketsBuild {

    private static final String TEA_GLFW_BACKEND_CLASS = "com.github.xpenatan.gdx.teavm.backends.glfw.config.backend.TeaGLFWBackend";
    private static final String TEA_GLFW_BUILD_TYPE_CLASS = TEA_GLFW_BACKEND_CLASS + "$NativeBuildType";
    private static final String TEA_BUILDER_CLASS = "com.github.xpenatan.gdx.teavm.backends.shared.config.builder.TeaBuilder";
    private static final String TEAVM_OPTIMIZATION_LEVEL_CLASS = "org.teavm.vm.TeaVMOptimizationLevel";
    public static final int DEFAULT_MIN_HEAP_SIZE = 64 * 1024 * 1024;
    public static final int DEFAULT_MAX_HEAP_SIZE = 512 * 1024 * 1024;
    public static final int DEFAULT_MIN_DIRECT_BUFFER_SIZE = 64 * 1024 * 1024;
    public static final File DEFAULT_OUTPUT_ROOT = new File("build/dist");
    public static final String DEFAULT_RELEASE_PATH = "c/release";
    public static final String LINUX_CURL_PROPERTY = "gdxTeaVMLinuxCurlPath";
    public static final String LINUX_CURL_ENV = "GDX_TEAVM_LINUX_CURL_PATH";
    public static final String LINUX_CURL_OUTPUT_NAME = "libcurl.so.4";
    public static final String MAC_CURL_PROPERTY = "gdxTeaVMMacCurlPath";
    public static final String MAC_CURL_ENV = "GDX_TEAVM_MAC_CURL_PATH";
    public static final String MAC_CURL_OUTPUT_NAME = "libcurl.4.dylib";

    private GLFWWebSocketsBuild() {
    }

    public static Builder builder(Class<?> mainClass) {
        return builder(mainClass.getName());
    }

    public static Builder builder(String mainClass) {
        return new Builder().setMainClass(mainClass);
    }

    public static void build(Class<?> mainClass, String[] args) throws IOException {
        builder(mainClass).build(args);
    }

    public static void build(String outputName, Class<?> mainClass, String[] args) throws IOException {
        builder(mainClass).setOutputName(outputName).build(args);
    }

    public static void packageHostCurlRuntime(File releaseDirectory) throws IOException {
        packageHostCurlRuntime(releaseDirectory.toPath());
    }

    public static void packageHostCurlRuntime(Path releaseDirectory) throws IOException {
        if(isLinuxHost()) {
            packageConfiguredCurlRuntime(resolveConfiguredPath(LINUX_CURL_PROPERTY, LINUX_CURL_ENV),
                    releaseDirectory,
                    LINUX_CURL_OUTPUT_NAME,
                    "Linux");
            return;
        }

        if(isMacHost()) {
            packageConfiguredCurlRuntime(resolveConfiguredPath(MAC_CURL_PROPERTY, MAC_CURL_ENV),
                    releaseDirectory,
                    MAC_CURL_OUTPUT_NAME,
                    "macOS");
        }
    }

    private static void packageConfiguredCurlRuntime(String configuredPath, Path releaseDirectory, String outputName, String platformName) throws IOException {
        if(configuredPath == null) {
            return;
        }

        Path sourcePath = Path.of(configuredPath);
        if(!Files.isRegularFile(sourcePath)) {
            throw new IllegalArgumentException("Configured libcurl runtime was not found: " + configuredPath);
        }

        Files.createDirectories(releaseDirectory);
        Path targetPath = releaseDirectory.resolve(outputName);
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Packaged " + platformName + " libcurl runtime: " + sourcePath + " -> " + targetPath);
    }

    private static String resolveConfiguredPath(String propertyName, String envName) {
        String configuredProperty = System.getProperty(propertyName);
        if(configuredProperty != null && !configuredProperty.isBlank()) {
            return configuredProperty.trim();
        }
        String configuredEnv = System.getenv(envName);
        if(configuredEnv != null && !configuredEnv.isBlank()) {
            return configuredEnv.trim();
        }
        return null;
    }

    private static boolean isLinuxHost() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase(Locale.ROOT).contains("linux");
    }

    private static boolean isMacHost() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase(Locale.ROOT).contains("mac");
    }

    public enum BuildAction {
        GENERATE(false, false),
        BUILD(true, false),
        RUN(true, true);

        private final boolean buildExecutable;
        private final boolean runExecutable;

        BuildAction(boolean buildExecutable, boolean runExecutable) {
            this.buildExecutable = buildExecutable;
            this.runExecutable = runExecutable;
        }

        private static BuildAction fromArg(String arg) {
            String normalized = normalize(arg);
            if(normalized.equals("generate")) {
                return GENERATE;
            }
            if(normalized.equals("build")) {
                return BUILD;
            }
            if(normalized.equals("run")) {
                return RUN;
            }
            throw new IllegalArgumentException("Unsupported GLFW action argument: " + arg);
        }
    }

    public static class Builder {

        private String mainClass;
        private String outputName;
        private File outputRoot = DEFAULT_OUTPUT_ROOT;
        private File releasePath;
        private String buildType = "Debug";
        private BuildAction buildAction = BuildAction.GENERATE;
        private boolean consoleLog;
        private boolean packageHostCurlRuntime = true;
        private boolean obfuscated;
        private String optimizationLevel = "FULL";
        private int minHeapSize = DEFAULT_MIN_HEAP_SIZE;
        private int maxHeapSize = DEFAULT_MAX_HEAP_SIZE;
        private int minDirectBufferSize = DEFAULT_MIN_DIRECT_BUFFER_SIZE;

        private Builder() {
        }

        public void build(String[] args) throws IOException {
            configureFromArgs(args);
            build();
        }

        public void build() throws IOException {
            if(mainClass == null || mainClass.isBlank()) {
                throw new IllegalStateException("A TeaVM GLFW main class is required.");
            }
            File effectiveReleasePath = getReleasePath();
            if(packageHostCurlRuntime) {
                packageHostCurlRuntime(effectiveReleasePath);
            }
            buildWithTeaVM(effectiveReleasePath);
        }

        public Builder configureFromArgs(String[] args) {
            if(args == null) {
                return this;
            }
            if(args.length > 3) {
                throw new IllegalArgumentException("Expected arguments: [Debug|Release] [generate|build|run] [console]");
            }
            if(args.length > 0) {
                setBuildType(args[0]);
            }
            if(args.length > 1) {
                setBuildAction(args[1]);
            }
            if(args.length > 2) {
                setRunExecutableWithConsoleLog(parseConsoleArg(args[2]));
            }
            if(consoleLog && buildAction != BuildAction.RUN) {
                throw new IllegalArgumentException("Console logging requires the run argument");
            }
            if(isDebugBuild() && buildAction == BuildAction.RUN) {
                consoleLog = true;
            }
            return this;
        }

        public Builder setMainClass(Class<?> mainClass) {
            return setMainClass(mainClass.getName());
        }

        public Builder setMainClass(String mainClass) {
            if(mainClass == null || mainClass.isBlank()) {
                throw new IllegalArgumentException("mainClass cannot be blank.");
            }
            this.mainClass = mainClass.trim();
            return this;
        }

        public Builder setOutputName(String outputName) {
            if(outputName == null || outputName.isBlank()) {
                throw new IllegalArgumentException("outputName cannot be blank.");
            }
            this.outputName = outputName.trim();
            return this;
        }

        public Builder setOutputRoot(File outputRoot) {
            if(outputRoot == null) {
                throw new IllegalArgumentException("outputRoot cannot be null.");
            }
            this.outputRoot = outputRoot;
            return this;
        }

        public Builder setOutputRoot(Path outputRoot) {
            return setOutputRoot(outputRoot.toFile());
        }

        public Builder setReleasePath(File releasePath) {
            if(releasePath == null) {
                throw new IllegalArgumentException("releasePath cannot be null.");
            }
            this.releasePath = releasePath;
            return this;
        }

        public Builder setReleasePath(Path releasePath) {
            return setReleasePath(releasePath.toFile());
        }

        public Builder setBuildType(String buildType) {
            String normalized = normalize(buildType);
            if(normalized.equals("debug")) {
                this.buildType = "Debug";
                return this;
            }
            if(normalized.equals("release")) {
                this.buildType = "Release";
                return this;
            }
            throw new IllegalArgumentException("Unsupported GLFW native build type: " + buildType);
        }

        public Builder setBuildType(Object buildType) {
            if(buildType == null) {
                throw new IllegalArgumentException("buildType cannot be null.");
            }
            return setBuildType(buildType.toString());
        }

        public Builder setBuildAction(BuildAction buildAction) {
            if(buildAction == null) {
                throw new IllegalArgumentException("buildAction cannot be null.");
            }
            this.buildAction = buildAction;
            return this;
        }

        public Builder setBuildAction(String buildAction) {
            return setBuildAction(BuildAction.fromArg(buildAction));
        }

        public Builder setBuildExecutableAfterBuild(boolean buildExecutableAfterBuild) {
            buildAction = buildExecutableAfterBuild ? BuildAction.BUILD : BuildAction.GENERATE;
            return this;
        }

        public Builder setRunExecutableAfterBuild(boolean runExecutableAfterBuild) {
            buildAction = runExecutableAfterBuild ? BuildAction.RUN : BuildAction.BUILD;
            return this;
        }

        public Builder setRunExecutableWithConsoleLog(boolean consoleLog) {
            this.consoleLog = consoleLog;
            return this;
        }

        public Builder setPackageHostCurlRuntime(boolean packageHostCurlRuntime) {
            this.packageHostCurlRuntime = packageHostCurlRuntime;
            return this;
        }

        public Builder setObfuscated(boolean obfuscated) {
            this.obfuscated = obfuscated;
            return this;
        }

        public Builder setOptimizationLevel(String optimizationLevel) {
            if(optimizationLevel == null || optimizationLevel.isBlank()) {
                throw new IllegalArgumentException("optimizationLevel cannot be blank.");
            }
            this.optimizationLevel = optimizationLevel.trim().toUpperCase(Locale.ROOT);
            return this;
        }

        public Builder setOptimizationLevel(Object optimizationLevel) {
            if(optimizationLevel == null) {
                throw new IllegalArgumentException("optimizationLevel cannot be null.");
            }
            return setOptimizationLevel(optimizationLevel.toString());
        }

        public Builder setMinHeapSize(int minHeapSize) {
            this.minHeapSize = minHeapSize;
            return this;
        }

        public Builder setMaxHeapSize(int maxHeapSize) {
            this.maxHeapSize = maxHeapSize;
            return this;
        }

        public Builder setMinDirectBufferSize(int minDirectBufferSize) {
            this.minDirectBufferSize = minDirectBufferSize;
            return this;
        }

        private File getReleasePath() {
            if(releasePath != null) {
                return releasePath;
            }
            return new File(outputRoot, DEFAULT_RELEASE_PATH);
        }

        private String getOutputName() {
            if(outputName != null) {
                return outputName;
            }
            return deriveOutputName(mainClass);
        }

        private static boolean parseConsoleArg(String arg) {
            String normalized = normalize(arg);
            if(normalized.equals("console")) {
                return true;
            }
            throw new IllegalArgumentException("Unsupported GLFW console argument: " + arg);
        }

        private static String deriveOutputName(String mainClass) {
            String simpleName = mainClass;
            int dotIndex = simpleName.lastIndexOf('.');
            if(dotIndex != -1) {
                simpleName = simpleName.substring(dotIndex + 1);
            }
            if(simpleName.endsWith("Launcher") && simpleName.length() > "Launcher".length()) {
                simpleName = simpleName.substring(0, simpleName.length() - "Launcher".length());
            }
            String normalized = simpleName
                    .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                    .replaceAll("[^A-Za-z0-9._-]+", "-")
                    .toLowerCase(Locale.ROOT);
            if(normalized.isBlank()) {
                return "app";
            }
            return normalized;
        }

        private void buildWithTeaVM(File effectiveReleasePath) {
            try {
                Class<?> teaGLFWBackendClass = Class.forName(TEA_GLFW_BACKEND_CLASS);
                Object cBackend = teaGLFWBackendClass.getConstructor().newInstance();
                invoke(cBackend, "setBuildType", Class.forName(TEA_GLFW_BUILD_TYPE_CLASS), enumValue(TEA_GLFW_BUILD_TYPE_CLASS, buildType.toUpperCase(Locale.ROOT)));
                invoke(cBackend, "setBuildExecutableAfterBuild", boolean.class, buildAction.buildExecutable);
                invoke(cBackend, "setRunExecutableAfterBuild", boolean.class, buildAction.runExecutable);
                invoke(cBackend, "setRunExecutableWithConsoleLog", boolean.class, consoleLog);

                Class<?> teaBuilderClass = Class.forName(TEA_BUILDER_CLASS);
                Constructor<?> teaBuilderConstructor = teaBuilderClass.getConstructor(teaGLFWBackendClass.getSuperclass());
                Object teaBuilder = teaBuilderConstructor.newInstance(cBackend);
                invoke(teaBuilder, "setOutputName", String.class, getOutputName());
                invoke(teaBuilder, "setObfuscated", boolean.class, obfuscated);
                invoke(teaBuilder, "setOptimizationLevel", Class.forName(TEAVM_OPTIMIZATION_LEVEL_CLASS), enumValue(TEAVM_OPTIMIZATION_LEVEL_CLASS, optimizationLevel));
                invoke(teaBuilder, "setMinHeapSize", int.class, minHeapSize);
                invoke(teaBuilder, "setMaxHeapSize", int.class, maxHeapSize);
                invoke(teaBuilder, "setMinDirectBuffersSize", int.class, minDirectBufferSize);
                invoke(teaBuilder, "setMainClass", String.class, mainClass);
                invoke(teaBuilder, "setReleasePath", File.class, effectiveReleasePath);
                invoke(teaBuilder, "build", File.class, outputRoot);
            }
            catch(ReflectiveOperationException e) {
                throw new IllegalStateException("gdx-teavm backend-glfw is required on the build classpath to use GLFWWebSocketsBuild, and its builder API must be compatible.", e);
            }
        }

        private boolean isDebugBuild() {
            return "Debug".equalsIgnoreCase(buildType);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(String enumClassName, String value) throws ClassNotFoundException {
        Class<?> enumClass = Class.forName(enumClassName);
        return Enum.valueOf(enumClass.asSubclass(Enum.class), value);
    }

    private static void invoke(Object target, String methodName, Class<?> argumentType, Object argument) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName, argumentType);
        method.invoke(target, argument);
    }

    private static String normalize(String arg) {
        return stripOptionPrefix(arg).toLowerCase(Locale.ROOT);
    }

    private static String stripOptionPrefix(String arg) {
        if(arg == null) {
            return "";
        }
        String normalized = arg.trim();
        while(normalized.startsWith("-")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
