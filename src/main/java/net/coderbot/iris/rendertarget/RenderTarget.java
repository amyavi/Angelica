package net.coderbot.iris.rendertarget;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryStack.*;

import com.gtnewhorizon.gtnhlib.bytebuf.MemoryStack;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.RenderSystem;
import lombok.Getter;
import net.coderbot.iris.gl.texture.InternalTextureFormat;
import net.coderbot.iris.gl.texture.PixelFormat;
import net.coderbot.iris.gl.texture.PixelType;
import org.joml.Vector2i;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class RenderTarget {
	@Getter
    private final InternalTextureFormat internalFormat;
	private final PixelFormat format;
	private final PixelType type;
	@Getter
    private int width;
	@Getter
    private int height;

	private boolean isValid;
	private final int mainTexture;
	private final int altTexture;

	private static final ByteBuffer NULL_BUFFER = null;

	public RenderTarget(Builder builder) {
		this.isValid = true;

		this.internalFormat = builder.internalFormat;
		this.format = builder.format;
		this.type = builder.type;

		this.width = builder.width;
		this.height = builder.height;

        try (MemoryStack stack = stackPush()) {
            IntBuffer textures = stack.mallocInt(2);
            GL11.glGenTextures(textures);

            this.mainTexture = textures.get(0);
            this.altTexture = textures.get(1);
        }

		boolean isPixelFormatInteger = builder.internalFormat.getPixelFormat().isInteger();
		setupTexture(mainTexture, builder.width, builder.height, !isPixelFormatInteger);
		setupTexture(altTexture, builder.width, builder.height, !isPixelFormatInteger);

		// Clean up after ourselves
		// This is strictly defensive to ensure that other buggy code doesn't tamper with our textures
		GLStateManager.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	}

	private void setupTexture(int texture, int width, int height, boolean allowsLinear) {
		resizeTexture(texture, width, height);

		RenderSystem.texParameteri(texture, GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, allowsLinear ? GL11.GL_LINEAR : GL11.GL_NEAREST);
		RenderSystem.texParameteri(texture, GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, allowsLinear ? GL11.GL_LINEAR : GL11.GL_NEAREST);
		RenderSystem.texParameteri(texture, GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		RenderSystem.texParameteri(texture, GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
	}

	private void resizeTexture(int texture, int width, int height) {
		RenderSystem.texImage2D(texture, GL11.GL_TEXTURE_2D, 0, internalFormat.getGlFormat(), width, height, 0, format.getGlFormat(), type.getGlFormat(), NULL_BUFFER);
	}

	void resize(Vector2i textureScaleOverride) {
		this.resize(textureScaleOverride.x, textureScaleOverride.y);
	}

	// Package private, call CompositeRenderTargets#resizeIfNeeded instead.
	void resize(int width, int height) {
		requireValid();

		this.width = width;
		this.height = height;

		resizeTexture(mainTexture, width, height);

		resizeTexture(altTexture, width, height);
	}

    public int getMainTexture() {
		requireValid();

		return mainTexture;
	}

	public int getAltTexture() {
		requireValid();

		return altTexture;
	}

    private final IntBuffer deleteBuffer = BufferUtils.createIntBuffer(2);
    public void destroy() {
		requireValid();
		isValid = false;
        deleteBuffer.put(0, mainTexture);
        deleteBuffer.put(1, altTexture);
        GLStateManager.glDeleteTextures(deleteBuffer);
	}

	private void requireValid() {
		if (!isValid) {
			throw new IllegalStateException("Attempted to use a deleted composite render target");
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private InternalTextureFormat internalFormat = InternalTextureFormat.RGBA8;
		private int width = 0;
		private int height = 0;
		private PixelFormat format = PixelFormat.RGBA;
		private PixelType type = PixelType.UNSIGNED_BYTE;

		private Builder() {
			// No-op
		}

		public Builder setInternalFormat(InternalTextureFormat format) {
			this.internalFormat = format;

			return this;
		}

		public Builder setDimensions(int width, int height) {
			if (width <= 0) {
				throw new IllegalArgumentException("Width must be greater than zero");
			}

			if (height <= 0) {
				throw new IllegalArgumentException("Height must be greater than zero");
			}

			this.width = width;
			this.height = height;

			return this;
		}

		public Builder setPixelFormat(PixelFormat pixelFormat) {
			this.format = pixelFormat;

			return this;
		}

		public Builder setPixelType(PixelType pixelType) {
			this.type = pixelType;

			return this;
		}

		public RenderTarget build() {
			return new RenderTarget(this);
		}
	}
}
