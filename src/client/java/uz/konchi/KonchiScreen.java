package uz.konchi;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** O'ng Shift bilan ochiladigan sozlamalar menyusi. */
public class KonchiScreen extends Screen {
	private static final int W = 200;
	private static final int H = 20;
	private static final int GAP = 24;

	public KonchiScreen() {
		super(Component.literal("Konchi"));
	}

	@Override
	protected void init() {
		int x = (this.width - W) / 2;
		int y = this.height / 2 - 70;

		toggle(x, y, "Fullbright", () -> Settings.fullbright, v -> Settings.fullbright = v);
		toggle(x, y + GAP, "Rudalar", () -> Settings.ores, v -> Settings.ores = v);
		toggle(x, y + GAP * 2, "Sandiqlar", () -> Settings.containers, v -> Settings.containers = v);
		toggle(x, y + GAP * 3, "O'yinchilar", () -> Settings.players, v -> Settings.players = v);

		this.addRenderableWidget(Button.builder(radiusLabel(), btn -> {
			Settings.nextRadius();
			OreScanner.reset();
			btn.setMessage(radiusLabel());
		}).bounds(x, y + GAP * 4, W, H).build());

		this.addRenderableWidget(Button.builder(Component.literal("Yopish"), btn -> this.onClose())
				.bounds(x, y + GAP * 5 + 8, W, H).build());
	}

	private void toggle(int x, int y, String name, BooleanSupplier getter, Consumer<Boolean> setter) {
		this.addRenderableWidget(Button.builder(label(name, getter.getAsBoolean()), btn -> {
			boolean value = !getter.getAsBoolean();
			setter.accept(value);
			btn.setMessage(label(name, value));
		}).bounds(x, y, W, H).build());
	}

	private static Component label(String name, boolean on) {
		return Component.literal(name + ": " + (on ? "§aYONIQ" : "§cO'CHIQ"));
	}

	private static Component radiusLabel() {
		return Component.literal("Ruda radiusi: " + Settings.oreRadius + " blok");
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 90, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		Settings.save();
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
