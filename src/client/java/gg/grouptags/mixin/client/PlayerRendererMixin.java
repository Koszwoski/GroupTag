package gg.grouptags.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.grouptags.client.GroupTag;
import gg.grouptags.client.GroupTagClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
abstract class PlayerRendererMixin {
    @Shadow @Final protected EntityRenderDispatcher entityRenderDispatcher;
    @Shadow @Final protected Font font;

    @Inject(method = "renderNameTag", at = @At("TAIL"))
    private void grouptag$renderGroupName(
        AbstractClientPlayer player,
        Component name,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        CallbackInfo ci
    ) {
        GroupTag groupTag = GroupTagClient.getTag(player.getUUID()).orElse(null);
        if (groupTag == null) {
            return;
        }

        Component groupName = Component.literal(groupTag.name());

        poseStack.pushPose();
        poseStack.translate(0.0F, player.getBbHeight() + 0.25F, 0.0F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(0.025F, -0.025F, 0.025F);

        Matrix4f matrix = poseStack.last().pose();
        float x = -this.font.width(groupName) / 2.0F;

        this.font.drawInBatch(
            groupName,
            x,
            0.0F,
            groupTag.color(),
            false,
            matrix,
            bufferSource,
            Font.DisplayMode.SEE_THROUGH,
            0,
            packedLight
        );

        poseStack.popPose();
    }
}
