package com.rcomandor;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Свободная камера: игрок остаётся на месте, а камера — отдельная
 * невидимая клиентская сущность, которой управляют WASD, пробел, Shift и мышь.
 */
public final class FreeCam {

	private static final double[] SPEEDS = {0.1, 0.2, 0.35, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0};

	public static ArmorStand camera;
	public static boolean noclip = true;
	private static int speedIndex = 3;

	private FreeCam() {}

	public static boolean isActive() {
		return camera != null;
	}

	public static void toggle(Minecraft mc) {
		if (isActive()) {
			disable(mc);
		} else {
			enable(mc);
		}
	}

	private static void enable(Minecraft mc) {
		if (mc.player == null || mc.level == null) return;

		ArmorStand cam = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
		Vec3 eye = mc.player.getEyePosition();
		cam.setPos(eye.x, eye.y - cam.getEyeHeight(), eye.z);
		cam.setYRot(mc.player.getYRot());
		cam.setXRot(mc.player.getXRot());
		syncHead(cam);
		syncOld(cam);

		camera = cam;
		// Отключаем отсечение невидимых секций: иначе, когда камера пролетает
		// сквозь блоки, уже загруженные чанки пропадают и рисуются заново.
		mc.smartCull = false;
		mc.setCameraEntity(cam);
		RcomandorClient.message(mc, "rcomandor.msg.on");
	}

	public static void disable(Minecraft mc) {
		if (camera == null) return;
		camera = null;
		mc.smartCull = true;
		if (mc.player != null) {
			mc.setCameraEntity(mc.player);
		}
		RcomandorClient.message(mc, "rcomandor.msg.off");
	}

	public static void changeSpeed(Minecraft mc, int dir) {
		speedIndex = Math.max(0, Math.min(SPEEDS.length - 1, speedIndex + dir));
		RcomandorClient.message(mc, "rcomandor.msg.speed", String.valueOf(SPEEDS[speedIndex] * 20) + " бл/с");
	}

	public static void tick(Minecraft mc) {
		if (camera == null) return;

		// Выключаемся при выходе из мира, смене измерения, смерти/респавне.
		if (mc.player == null || mc.level == null || camera.level() != mc.level
				|| mc.getCameraEntity() != camera) {
			camera = null;
			mc.smartCull = true;
			if (mc.player != null && mc.getCameraEntity() == null) {
				mc.setCameraEntity(mc.player);
			}
			return;
		}

		Options o = mc.options;

		// Пока камера отделена, клики не уходят на сервер: игрок ничего не ломает и не использует.
		blockKey(o.keyAttack);
		blockKey(o.keyUse);
		blockKey(o.keyPickItem);

		syncOld(camera);

		if (mc.screen != null) return; // открыт чат/инвентарь — стоим на месте

		double forward = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
		double left = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
		double up = (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0);
		if (forward == 0 && left == 0 && up == 0) return;

		double yaw = Math.toRadians(camera.getYRot());
		double sin = Math.sin(yaw);
		double cos = Math.cos(yaw);
		Vec3 dir = new Vec3(left * cos - forward * sin, up, forward * cos + left * sin);
		if (dir.lengthSqr() > 1) dir = dir.normalize();

		double speed = SPEEDS[speedIndex] * (o.keySprint.isDown() ? 2.5 : 1.0);
		move(mc, dir.scale(speed));
	}

	private static void move(Minecraft mc, Vec3 delta) {
		double x = camera.getX();
		double y = camera.getY();
		double z = camera.getZ();

		if (noclip) {
			camera.setPos(x + delta.x, y + delta.y, z + delta.z);
			return;
		}

		// С коллизией двигаемся по каждой оси отдельно, чтобы скользить вдоль стен.
		double eye = camera.getEyeHeight();
		if (free(mc, x + delta.x, y + eye, z)) x += delta.x;
		if (free(mc, x, y + delta.y + eye, z)) y += delta.y;
		if (free(mc, x, y + eye, z + delta.z)) z += delta.z;
		camera.setPos(x, y, z);
	}

	private static boolean free(Minecraft mc, double x, double eyeY, double z) {
		return mc.level.noCollision(AABB.ofSize(new Vec3(x, eyeY, z), 0.3, 0.3, 0.3));
	}

	private static void blockKey(KeyMapping key) {
		while (key.consumeClick()) {
			// сбрасываем накопленные клики
		}
		key.setDown(false);
	}

	private static void syncOld(ArmorStand cam) {
		cam.xo = cam.getX();
		cam.yo = cam.getY();
		cam.zo = cam.getZ();
		cam.xOld = cam.getX();
		cam.yOld = cam.getY();
		cam.zOld = cam.getZ();
		cam.yRotO = cam.getYRot();
		cam.xRotO = cam.getXRot();
		cam.yHeadRotO = cam.yHeadRot;
		cam.yBodyRotO = cam.yBodyRot;
	}

	/**
	 * У стойки для брони Minecraft берёт поворот вида от головы, а не от тела,
	 * поэтому голову и тело всегда держим в том же направлении, что и камеру.
	 */
	public static void syncHead(ArmorStand cam) {
		cam.yHeadRot = cam.getYRot();
		cam.yBodyRot = cam.getYRot();
	}

	/** Вызывается при движении мыши. */
	public static void turn(double yRot, double xRot) {
		if (camera == null) return;
		float oldYaw = camera.getYRot();
		camera.turn(yRot, xRot);
		float delta = camera.getYRot() - oldYaw;
		camera.yHeadRot += delta;
		camera.yHeadRotO += delta;
		camera.yBodyRot = camera.yHeadRot;
		camera.yBodyRotO = camera.yHeadRotO;
	}
}
