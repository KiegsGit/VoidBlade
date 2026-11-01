package com.voidblade;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Finds a standable, non-hazardous spot near a wanted position. Returns null if there is none. */
public final class SafeTeleport {
	private static final int SEARCH = 12;

	public static Vec3 find(ServerLevel level, Vec3 want) {
		BlockPos base = BlockPos.containing(want);
		level.getChunkAt(base); // make sure the destination chunk is loaded
		for (int d = 0; d <= SEARCH; d++) {
			for (int sign = 0; sign < (d == 0 ? 1 : 2); sign++) {
				int dy = sign == 0 ? d : -d;
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						BlockPos p = base.offset(dx, dy, dz);
						if (ok(level, p)) {
							boolean exact = dx == 0 && dy == 0 && dz == 0;
							return new Vec3(exact ? want.x : p.getX() + 0.5, exact ? want.y : p.getY(), exact ? want.z : p.getZ() + 0.5);
						}
					}
				}
			}
		}
		return null;
	}

	private static boolean ok(ServerLevel l, BlockPos feet) {
		BlockPos head = feet.above();
		BlockPos floor = feet.below();
		return l.isInWorldBounds(feet) && l.isInWorldBounds(head) && l.getWorldBorder().isWithinBounds(feet)
			&& free(l, feet) && free(l, head) && solid(l, floor);
	}

	private static boolean free(ServerLevel l, BlockPos p) {
		BlockState s = l.getBlockState(p);
		return s.getCollisionShape(l, p).isEmpty() && l.getFluidState(p).isEmpty()
			&& !s.is(Blocks.FIRE) && !s.is(Blocks.SOUL_FIRE) && !s.is(Blocks.COBWEB) && !s.is(Blocks.SWEET_BERRY_BUSH);
	}

	private static boolean solid(ServerLevel l, BlockPos p) {
		BlockState s = l.getBlockState(p);
		return !s.getCollisionShape(l, p).isEmpty()
			&& !s.is(Blocks.MAGMA_BLOCK) && !s.is(Blocks.CACTUS) && !s.is(Blocks.CAMPFIRE) && !s.is(Blocks.SOUL_CAMPFIRE);
	}

	private SafeTeleport() {
	}
}
