package gg.grouptags.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.grouptags.client.GroupTag;
import gg.grouptags.client.GroupTagClient;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(EntityRenderer.class)
abstract class PlayerRendererMixin {
    @Unique private final Map<EntityRenderState, GroupTag> grouptag$tags = new WeakHashMap<>();
    @Unique private boolean grouptag$submitting;

    @Shadow protected abstract void submitNameTag(EntityRenderState state, PoseStack poses,
        SubmitNodeCollector collector, CameraRenderState camera);

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void grouptag$extract(Entity entity, EntityRenderState state, float tickDelta, CallbackInfo ci) {
        grouptag$tags.remove(state);
        if (entity instanceof AbstractClientPlayer) {
            GroupTagClient.getTag(entity.getUUID()).ifPresent(tag -> grouptag$tags.put(state, tag));
        }
    }

    @Inject(method = "submitNameTag", at = @At("TAIL"))
    private void grouptag$submit(EntityRenderState state, PoseStack poses,
        SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (grouptag$submitting || state.nameTag == null || state.nameTagAttachment == null) return;
        GroupTag tag = grouptag$tags.get(state);
        if (tag == null) return;
        Component originalName = state.nameTag;
        Vec3 originalAttachment = state.nameTagAttachment;
        grouptag$submitting = true;
        try {
            state.nameTag = Component.literal(tag.name()).withColor(tag.color() & 0xFFFFFF);
            state.nameTagAttachment = originalAttachment.add(0.0, -0.28, 0.0);
            submitNameTag(state, poses, collector, camera);
        } finally {
            state.nameTag = originalName;
            state.nameTagAttachment = originalAttachment;
            grouptag$submitting = false;
        }
    }
}
