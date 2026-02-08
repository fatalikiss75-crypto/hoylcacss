package ru.nbk.rolecases.particle;

import org.bukkit.Color;
import org.bukkit.Particle;

public record WrappedParticle(Particle particle, int count, double offX, double offY, double offZ, double speed, int red, int green, int blue) {

    public Object getData() {
        if (particle.getDataType() == Particle.DustOptions.class) {
            return new Particle.DustOptions(Color.fromRGB(red, green, blue), 1);
        }

        return null;
    }

}
