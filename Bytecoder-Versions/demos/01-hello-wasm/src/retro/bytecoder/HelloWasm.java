package retro.bytecoder;

public class HelloWasm {
    public static void main(String[] args) {
        System.out.println("=== Bytecoder WebAssembly GC Demo ===");
        System.out.println("Hello from Java compiled directly to WasmGC via Bytecoder!");
        
        long time = System.currentTimeMillis();
        System.out.println("Current System Time (ms): " + time);

        int sum = 0;
        for (int i = 1; i <= 100; i++) {
            sum += i;
        }
        System.out.println("Computed Gauss Sum (1..100): " + sum);

        String test = "Retro-Bytecoder-Engine";
        System.out.println("String test: length=" + test.length() + ", uppercase=" + test.toUpperCase());
        System.out.println("Bytecoder WasmGC execution verified successfully!");
    }
}
