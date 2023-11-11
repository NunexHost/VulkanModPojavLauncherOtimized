package net.vulkanmod.render.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.Buffer;
import net.vulkanmod.vulkan.memory.StagingBuffer;
import net.vulkanmod.vulkan.queue.CommandPool;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkMemoryBarrier;

import java.nio.LongBuffer;

import static net.vulkanmod.render.chunk.DrawBuffers.tVirtualBufferIdx;
import static net.vulkanmod.vulkan.queue.Queue.TransferQueue;
import static org.lwjgl.vulkan.VK10.*;

public class AreaUploadManager {
    public static final int FRAME_NUM = 2;
    public static AreaUploadManager INSTANCE;

    //TODO: Might replace this with custom implementation later (to allow faster Key Swapping)
    private final Long2ObjectArrayMap<ObjectArrayFIFOQueue<SubCopyCommand>> DistinctBuffers = new Long2ObjectArrayMap<>(8);
    private long[] fenceArray;

    public static void createInstance() {
        INSTANCE = new AreaUploadManager();
    }

    final ObjectArrayList<CommandPool.CommandBuffer> Submits = new ObjectArrayList<>(8);

    //    ObjectArrayList<virtualSegmentBuffer>[] recordedUploads;
    ObjectArrayList<DrawBuffers.ParametersUpdate>[] updatedParameters;
    ObjectArrayList<Runnable>[] frameOps;
    CommandPool.CommandBuffer[] commandBuffers;

    int currentFrame;

    public void init() {
        this.commandBuffers = new CommandPool.CommandBuffer[FRAME_NUM];
//        this.recordedUploads = new ObjectArrayList[FRAME_NUM];
        this.updatedParameters = new ObjectArrayList[FRAME_NUM];
        this.frameOps = new ObjectArrayList[FRAME_NUM];
        this.fenceArray = new long[FRAME_NUM];

        for (int i = 0; i < FRAME_NUM; i++) {
//            this.recordedUploads[i] = new ObjectArrayList<>();
            this.updatedParameters[i] = new ObjectArrayList<>();
            this.frameOps[i] = new ObjectArrayList<>();
            this.fenceArray[i] = createFence();
        }
    }

    private long createFence() {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer pFence = stack.mallocLong(1);
            vkCreateFence(Vulkan.getDevice(), fenceInfo, null, pFence);
            return pFence.get(0);
        }
    }

    public void editkey(long ik, long k) {
        if(!this.DistinctBuffers.containsKey(ik)) return;

        final ObjectArrayFIFOQueue<SubCopyCommand> aTstObjectArrayFIFOQueue = DistinctBuffers.remove(ik);

        //        for(var id : DistinctBuffers[currentFrame.values()) {
        //            id.clear();
        //        }
        this.DistinctBuffers.put(k, aTstObjectArrayFIFOQueue);
        //        waitUploads();
    }
    public void submitUploads() {
        if(!this.DistinctBuffers.isEmpty()) extracted1();
        if (this.Submits.isEmpty()) return;
        fenceArray[currentFrame] = TransferQueue.submitCommands2(Submits, fenceArray[currentFrame]);
    }

    private void extracted1() {
        beginIfNeeded();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            final long l = Vulkan.getStagingBuffer().getId();
            for (final var queueEntry : DistinctBuffers.long2ObjectEntrySet()) {
                final var value = queueEntry.getValue();
                final var copyRegions = VkBufferCopy.malloc(value.size(), stack);
                for (VkBufferCopy vkBufferCopy : copyRegions) {
                    SubCopyCommand a = value.dequeue();
                    vkBufferCopy.set(a.offset(), a.dstOffset(), a.bufferSize());
                }

                vkCmdCopyBuffer(commandBuffers[currentFrame].getHandle(), l, queueEntry.getLongKey(), copyRegions);
            }
        }

        this.DistinctBuffers.clear();

        Submits.add(0, commandBuffers[currentFrame]);
    }

    public void extracted() {
        if(tVirtualBufferIdx.isEmpty()) return;
        CommandPool.CommandBuffer commandBuffer = TransferQueue.beginCommands();
        tVirtualBufferIdx.uploadSubset(Vulkan.getStagingBuffer().getId(), commandBuffer);
        Submits.push(commandBuffer);
    }

    public SubCopyCommand uploadAsync2(long dstBufferSize, long dstOffset, long bufferSize, long src) {
        Validate.isTrue(dstOffset<dstBufferSize);


//            this.commandBuffers[currentFrame] = GraphicsQueue.getInstance().beginCommands();

        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer();
        stagingBuffer.copyBuffer2((int) bufferSize, src);

//        TransferQueue.uploadBufferCmd(this.commandBuffers[currentFrame], stagingBuffer.getId(), stagingBuffer.getOffset(), bufferId, dstOffset, bufferSize);
        return new SubCopyCommand(stagingBuffer.getOffset(), dstOffset, bufferSize);
    }

    private void beginIfNeeded() {
        if(commandBuffers[currentFrame] == null)
            this.commandBuffers[currentFrame] = TransferQueue.beginCommands();
    }

    public void uploadAsync(long bufferId, long dstOffset, long bufferSize, long src) {

//            this.commandBuffers[currentFrame] = Device.getGraphicsQueue().beginCommands();

        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer();
        stagingBuffer.copyBuffer2((int) bufferSize, src);

//        TransferQueue.uploadBufferCmd(this.commandBuffers[currentFrame], stagingBuffer.getId(), stagingBuffer.getOffset(), bufferId, dstOffset, bufferSize);
        //        this.recordedUploads[this.currentFrame].enqueue(aTst);
        if(!this.DistinctBuffers.containsKey(bufferId)) this.DistinctBuffers.put(bufferId, new ObjectArrayFIFOQueue<>());
        this.DistinctBuffers.get(bufferId).enqueue(new SubCopyCommand(stagingBuffer.getOffset(), dstOffset, bufferSize));
//        return aTst;
    }

//        if(!dstBuffers.add(bufferId)) {
//            try (MemoryStack stack = MemoryStack.stackPush()) {
//                VkMemoryBarrier.Buffer barrier = VkMemoryBarrier.calloc(1, stack);
//                barrier.sType$Default();
//                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
//                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
//
//                vkCmdPipelineBarrier(commandBuffer,
//                        VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
//                        0,
//                        barrier,
//                        null,
//                        null);
//            }
//
//
//            dstBuffers.clear();
//        }


    public void copyImmediate(Buffer src, Buffer dst) {
        if(dst.getBufferSize() < src.getBufferSize()) {
            throw new IllegalArgumentException("dst buffer is smaller than src buffer.");
        }

        CommandPool.CommandBuffer commandBuffer = TransferQueue.beginCommands();
        TransferQueue.uploadBufferCmd(commandBuffer.getHandle(), src.getId(), 0, dst.getId(), 0, src.getBufferSize());
        Synchronization.waitFence(TransferQueue.submitCommands(commandBuffer));
    }

    public void updateFrame() {
        waitUploads(this.currentFrame);
        this.currentFrame ^= this.currentFrame;
//        executeFrameOps(this.currentFrame);
    }


    public void waitUploads() {
        this.waitUploads(currentFrame);
    }
    private void waitUploads(int frame) {
        if(Submits.isEmpty()) return;

//        if(Synchronization.checkFenceStatus(fenceArray[currentFrame]))
        Synchronization.waitFence(fenceArray[currentFrame]);

        //        for(AreaBuffer.Segment uploadSegment : this.recordedUploads[frame]) {
//            uploadSegment.setReady();
//        }

        Submits.forEach(CommandPool.CommandBuffer::reset);

        for(DrawBuffers.ParametersUpdate parametersUpdate : this.updatedParameters[frame]) {
            parametersUpdate.setDrawParameters();
        }

//        this.commandBuffers[frame].reset();
        this.commandBuffers[frame] = null;
        Submits.clear();
//        this.recordedUploads[frame].clear();
    }

}
