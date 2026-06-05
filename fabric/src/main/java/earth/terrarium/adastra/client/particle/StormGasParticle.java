package earth.terrarium.adastra.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Fast, low-friction, no-collision tinted gas streaking on the wind during a planet storm.
 *
 * <p>Vanilla dust (the previous attempt) damps its velocity each tick and floats, so it never read
 * as "blown around". This particle keeps its full initial velocity for its whole life and ignores
 * terrain collision, so it streaks across the view like gas driven by the gale. Colour samples a
 * Jupiter gas palette; alpha eases in then fades out, and the quad is large for a hazy, view-
 * obscuring look. Spawned from {@code StormParticles} with a velocity along the wind direction.
 */
public class StormGasParticle extends SingleQuadParticle {

    private static final int[] GAS_COLORS = {
        0xF2E9D2, 0xF0EAD6, 0xE3C99A, 0xD8B88A,
        0xCAA56E, 0x8A5D37, 0xB5503A, 0xAFC6D6,
    };

    private final float baseAlpha;

    protected StormGasParticle(ClientLevel level, double x, double y, double z,
                               double xSpeed, double ySpeed, double zSpeed, TextureAtlasSprite sprite) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprite);
        this.hasPhysics = false;     // stream straight through terrain
        this.gravity = 0.0f;
        // The base Particle constructor adds random spread to the velocity; overwrite it so the gas
        // travels exactly along the wind vector we were given.
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.lifetime = 24 + this.random.nextInt(22);          // 24..45 ticks
        this.quadSize = 1.4f + this.random.nextFloat() * 2.2f; // big hazy puffs

        int c = GAS_COLORS[this.random.nextInt(GAS_COLORS.length)];
        this.setColor(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f);
        this.baseAlpha = 0.35f + this.random.nextFloat() * 0.30f;
        this.setAlpha(0.0f);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.move(this.xd, this.yd, this.zd); // constant velocity, no collision
        // Ease alpha in over the first 20% of life, fade out over the rest.
        float t = (float) this.age / (float) this.lifetime;
        float env = t < 0.2f ? (t / 0.2f) : (1.0f - (t - 0.2f) / 0.8f);
        this.setAlpha(Mth.clamp(this.baseAlpha * env, 0.0f, 1.0f));
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
            return new StormGasParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites.get(random));
        }
    }
}
