package net.vulkanmod.mixin.render.entity;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityRenderer.class)
public class EntityRendererM<T extends Entity> {

    /**
     * @author
     * @reason
     */
    public boolean shouldRender(T entity, Frustum frustum, double d, double e, double f, boolean useFrustumCulling) {
        if (!entity.shouldRender(d, e, f)) {
            return false;
        } else if (entity.noCulling) {
            return true;
        } else {
            // Early Culling
            if (entity instanceof HasVisibilityToFrustum) { // Assuming an interface for visibility check
                if (!((HasVisibilityToFrustum) entity).isVisibleTo(frustum)) {
                    return false;
                }
            } else {
                // Use alternative visibility logic for older versions
            }

            // Caching
            AABB aabb = entity.getBoundingBoxForCulling();
            WorldRenderer worldRenderer = WorldRenderer.getInstance();

            // Method Inlining
            boolean isVisible = frustum.isVisible(aabb);

            // Branching
            if (isVisible) {
                // Redundant Calls
                Vec3 pos = aabb.getCenter();

                // Null Checks
                RenderSection section = worldRenderer.getSectionGrid().getSectionAtBlockPos((int) pos.x(), (int) pos.y(), (int) pos.z());

                if (section == null) {
                    return isVisible;
                }

                return worldRenderer.getLastFrame() == section.getLastFrame();
            }

            return false;
        }
    }
}
