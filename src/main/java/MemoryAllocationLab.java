import java.io.*;
import java.util.*;

public class MemoryAllocationLab {

    static class MemoryBlock {
        int start;
        int size;
        String processName;  // null if free

        public MemoryBlock(int start, int size, String processName) {
            this.start = start;
            this.size = size;
            this.processName = processName;
        }

        public boolean isFree() {
            return processName == null;
        }

        public int getEnd() {
            return start + size - 1;
        }
    }

    static int totalMemory;
    static ArrayList<MemoryBlock> memory;
    static int successfulAllocations = 0;
    static int failedAllocations = 0;

    public static void processRequests(String filename) {
        memory = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            
            String line = br.readLine();
            if (line != null) {
                totalMemory = Integer.parseInt(line.trim());
                memory.add(new MemoryBlock(0, totalMemory, null));
                System.out.println("Initialized memory: " + totalMemory + " KB\n");
            }

            
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                
                if (parts[0].equals("REQUEST")) {
                    String processName = parts[1];
                    int size = Integer.parseInt(parts[2]);
                    allocate(processName, size);
                } else if (parts[0].equals("RELEASE")) {
                    String processName = parts[1];
                    deallocate(processName);
                }
            }

        } catch (FileNotFoundException e) {
            System.err.println("Error: File not found - " + filename);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Error parsing number: " + e.getMessage());
        }

    }

    
    private static void allocate(String processName, int size) {
        for (int i = 0; i < memory.size(); i++) {
            MemoryBlock block = memory.get(i);
            
            if (block.isFree() && block.size >= size) {
                block.processName = processName;
                
                if (block.size > size) {
                    int remainingSize = block.size - size;
                    int remainingStart = block.start + size;
            
                    block.size = size;
                    
                    MemoryBlock freeBlock = new MemoryBlock(remainingStart, remainingSize, null);
                    memory.add(i + 1, freeBlock);
                }
                
                successfulAllocations++;
                System.out.printf("SUCCESS: Allocated %d KB to %s at position %d\n", 
                                  size, processName, block.start);
                return;
            }
        }
        
        failedAllocations++;
        System.out.printf("FAILED: Could not allocate %d KB to %s (insufficient memory)\n", 
                          size, processName);

    }
    private static void deallocate(String processName) {
        for (int i = 0; i < memory.size(); i++) {
            MemoryBlock block = memory.get(i);
            
            if (!block.isFree() && block.processName.equals(processName)) {
                block.processName = null; 
                System.out.printf("RELEASED: %d KB from %s at position %d\n", 
                                  block.size, processName, block.start);
                
                // Merge with adjacent free blocks
                mergeAdjacentFreeBlocks(i);
                return;
            }
        }
        
        System.out.printf("WARNING: Process %s not found for deallocation\n", processName);
    }

    private static void mergeAdjacentFreeBlocks(int index) {
        // Merge with next block if it's free
        while (index < memory.size() - 1) {
            MemoryBlock current = memory.get(index);
            MemoryBlock next = memory.get(index + 1);
            
            if (current.isFree() && next.isFree()) {
                current.size += next.size;
                memory.remove(index + 1);
            } else {
                break;
            }
        }
        
        // Merge with previous block if it's free
        while (index > 0) {
            MemoryBlock current = memory.get(index);
            MemoryBlock prev = memory.get(index - 1);
            
            if (current.isFree() && prev.isFree()) {
                prev.size += current.size;
                memory.remove(index);
                index--;
            } else {
                break;
            }
        }
    }

    public static void displayStatistics() {
        System.out.println("\n========================================");
        System.out.println("Final Memory State");
        System.out.println("========================================");

        int blockNum = 1;
        for (MemoryBlock block : memory) {
            String status = block.isFree() ? "FREE" : block.processName;
            String allocated = block.isFree() ? "" : " - ALLOCATED";
            System.out.printf("Block %d: [%d-%d]%s%s (%d KB)%s\n",
                    blockNum++,
                    block.start,
                    block.getEnd(),
                    " ".repeat(Math.max(1, 10 - String.valueOf(block.getEnd()).length())),
                    status,
                    block.size,
                    allocated);
        }

        System.out.println("\n========================================");
        System.out.println("Memory Statistics");
        System.out.println("========================================");

        int allocatedMem = 0;
        int freeMem = 0;
        int numProcesses = 0;
        int numFreeBlocks = 0;
        int largestFree = 0;

        for (MemoryBlock block : memory) {
            if (block.isFree()) {
                freeMem += block.size;
                numFreeBlocks++;
                largestFree = Math.max(largestFree, block.size);
            } else {
                allocatedMem += block.size;
                numProcesses++;
            }
        }

        double allocatedPercent = (allocatedMem * 100.0) / totalMemory;
        double freePercent = (freeMem * 100.0) / totalMemory;
        double fragmentation = freeMem > 0 ?
                ((freeMem - largestFree) * 100.0) / freeMem : 0;

        System.out.printf("Total Memory:           %d KB\n", totalMemory);
        System.out.printf("Allocated Memory:       %d KB (%.2f%%)\n", allocatedMem, allocatedPercent);
        System.out.printf("Free Memory:            %d KB (%.2f%%)\n", freeMem, freePercent);
        System.out.printf("Number of Processes:    %d\n", numProcesses);
        System.out.printf("Number of Free Blocks:  %d\n", numFreeBlocks);
        System.out.printf("Largest Free Block:     %d KB\n", largestFree);
        System.out.printf("External Fragmentation: %.2f%%\n", fragmentation);

        System.out.println("\nSuccessful Allocations: " + successfulAllocations);
        System.out.println("Failed Allocations:     " + failedAllocations);
        System.out.println("========================================");
    }

    /**
     * Main method (FULLY PROVIDED)
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java MemoryAllocationLab <input_file>");
            System.out.println("Example: java MemoryAllocationLab memory_requests.txt");
            return;
        }

        System.out.println("========================================");
        System.out.println("Memory Allocation Simulator (First-Fit)");
        System.out.println("========================================\n");
        System.out.println("Reading from: " + args[0]);

        processRequests(args[0]);
        displayStatistics();
    }
}