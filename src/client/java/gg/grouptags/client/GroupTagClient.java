package gg.grouptags.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.Optional;
import java.util.UUID;

public final class GroupTagClient implements ClientModInitializer {
    private static final TagService TAGS = new TagService();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(TAGS::tick);
    }

    public static Optional<GroupTag> getTag(UUID playerUuid) {
        return TAGS.get(playerUuid);
    }
}
