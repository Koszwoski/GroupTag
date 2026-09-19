package gg.grouptags.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.grouptags.client.GroupTag;
import gg.grouptags.client.GroupTagClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.WeakHashMap;

@Mixin(AvatarRenderer.class)
abstract class PlayerRendererMixin {
    @Unique private final Map<AvatarRenderState, GroupTag> grouptag$tags = new WeakHashMap<>();
    @Unique private boolean grouptag$submitting;
    @Unique private final java.util.Set<java.util.UUID> grouptag$seen = new java.util.HashSet<>();
    @Unique private boolean grouptag$reportedSubmit;

    @Shadow
    protected abstract void submitNameTag(AvatarRenderState state, PoseStack poses,
                                          SubmitNodeCollector collector, CameraRenderState camera);

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void grouptag$extract(Avatar player, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        grouptag$tags.remove(state);
        GroupTagClient.getTag(player.getUUID()).ifPresent(tag -> {
            grouptag$tags.put(state, tag);
            if (grouptag$seen.add(player.getUUID())) {
                org.slf4j.LoggerFactory.getLogger("GroupTag").info(
                    "[GroupTag] Player renderer matched {} to {}", player.getUUID(), tag.name());
            }
        });
    }

    @Inject(method = "submitNameTag", at = @At("TAIL"))
    private void grouptag$submit(AvatarRenderState state, PoseStack poses,
                                 SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (grouptag$submitting || state.nameTag == null || state.nameTagAttachment == null) {
            return;
        }

        GroupTag tag = grouptag$tags.get(state);
        if (tag == null) {
            return;
        }

        if (!grouptag$reportedSubmit) {
            org.slf4j.LoggerFactory.getLogger("GroupTag").info(
                "[GroupTag] Submitting group nametag: {}", tag.name());
            grouptag$reportedSubmit = true;
        }

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
