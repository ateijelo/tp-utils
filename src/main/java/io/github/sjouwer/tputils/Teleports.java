package io.github.sjouwer.tputils;

import io.github.sjouwer.tputils.config.ModConfig;
import io.github.sjouwer.tputils.util.BlockCheck;
import io.github.sjouwer.tputils.util.InfoProvider;
import io.github.sjouwer.tputils.util.RaycastUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class Teleports {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final ModConfig config = TpUtils.getConfig();

    private Teleports() {
    }

    public static void tpThrough() {
        HitResult hit = RaycastUtil.forwardFromPlayer(config.getTpThroughRange());

        MutableText error;
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = BlockCheck.findOpenSpotForwards(hit, config.getTpThroughRange());
            if (pos != null) {
                tpToBlockPos(pos);
                return;
            }
            error = Text.translatable("text.tputils.message.tooMuchWall");
        }
        else {
            error = Text.translatable("text.tputils.message.noObstacleFound");
        }

        InfoProvider.sendError(error);
    }

    public static void tpOnTop(HitResult hit) {
        if (hit == null) {
            hit = RaycastUtil.forwardFromPlayer(config.getTpOnTopRange());
        }

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = ((BlockHitResult)hit).getBlockPos();
            BlockPos tpPos = BlockCheck.findTopOpenSpot(hitPos);
            if (tpPos != null) {
                tpToBlockPos(tpPos);
                return;
            }
        }

        InfoProvider.sendError(Text.translatable("text.tputils.message.noBlockFound"));
    }

    public static void tpForward() {
        HitResult hit = RaycastUtil.forwardFromPlayer(config.getTpForwardRange());
        double distance = client.cameraEntity.getEyePos().distanceTo(hit.getPos());
        BlockPos pos = BlockCheck.findOpenSpotBackwards(hit, distance);

        MutableText error;
        if (pos != null) {
            BlockPos playerPos = BlockPos.ofFloored(client.player.getPos());
            if (!pos.equals(playerPos)) {
                tpToBlockPos(pos);
                return;
            }
            error = Text.translatable("text.tputils.message.cantMoveForward");
        }
        else {
            error = Text.translatable("text.tputils.message.obstructed");
        }

        InfoProvider.sendError(error);
    }

    public static void tpGround(HitResult hit) {
        if (hit == null) {
            hit = RaycastUtil.downwardFromPlayer(config.isLavaAllowed());
        }

        MutableText error;
        if (hit.getPos().getY() == client.player.getPos().getY()) {
            error = Text.translatable("text.tputils.message.alreadyGrounded");
        }
        else if (hit.getPos().getY() == client.world.getBottomY()) {
            error = Text.translatable("text.tputils.message.noGroundFound");
        }
        else {
            tpToExactPos(hit.getPos());
            return;
        }

        InfoProvider.sendError(error);
    }

    public static void tpUp() {
        HitResult hit = RaycastUtil.upwardFromPlayer();
        if (hit.getPos().y < client.world.getHeight()){
            tpOnTop(hit);
            return;
        }

        InfoProvider.sendError(Text.translatable("text.tputils.message.nothingAbove"));
    }

    public static void tpDown() {
        HitResult hit = RaycastUtil.downwardFromPlayer(false);

        MutableText error;
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = ((BlockHitResult)hit).getBlockPos();
            BlockPos bottomPos = BlockCheck.findBottomOpenSpot(hitPos);
            if (bottomPos != null && bottomPos.getY() > client.world.getBottomY()) {
                hit = RaycastUtil.downwardFromPos(bottomPos, false);
                tpGround(hit);
                return;
            }
            error = Text.translatable("text.tputils.message.noOpenSpaceBelow");
        }
        else {
            error = Text.translatable("text.tputils.message.nothingBelow");
        }

        InfoProvider.sendError(error);
    }

    public static void tpBack() {
        Vec3d coordinates = config.getPreviousLocation();
        if (coordinates != null) {
            tpToExactPos(coordinates);
        }
        else {
            InfoProvider.sendError(Text.translatable("text.tputils.message.noPreviousLocation"));
        }
    }

    public static void chunkTp(int x, int y, int z) {
        double xPos = x * 16 + 8.0;
        double yPos = y * 16 + 8.0;
        double zPos = z * 16 + 8.0;

        tpToExactPos(new Vec3d(xPos, yPos, zPos));
    }

    private static void tpToBlockPos(BlockPos pos) {
        config.setPreviousLocation(client.player.getPos());
        client.getNetworkHandler().sendCommand(config.getTpMethod() + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
    }

    private static void tpToExactPos(Vec3d pos) {
        if (config.getTpMethod().equals("tp") || config.getTpMethod().equals("minecraft:tp")) {
            config.setPreviousLocation(client.player.getPos());
            client.getNetworkHandler().sendCommand(config.getTpMethod() + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
        }
        else {
            BlockPos blockPos = BlockPos.ofFloored(pos.getX(), Math.ceil(pos.getY()), pos.getZ());
            tpToBlockPos(blockPos);
        }
    }
}
