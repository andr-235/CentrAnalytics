package com.ca.centranalytics.integration.channel.vk;

import com.ca.centranalytics.integration.channel.vk.config.VkAutoCollectionProperties;
import com.ca.centranalytics.integration.channel.vk.service.VkAutoCollectionScheduler;
import com.ca.centranalytics.integration.channel.vk.service.VkAutoCollectionService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VkAutoCollectionSchedulerTest {

    @Test
    void skipsCollectionWhenAutoCollectionIsDisabled() {
        RecordingVkAutoCollectionService service = new RecordingVkAutoCollectionService();
        VkAutoCollectionScheduler scheduler = new VkAutoCollectionScheduler(
                new VkAutoCollectionProperties(false, "Primorsky Krai", 25, 10, 5, 20, "HYBRID", 900000L),
                service
        );

        scheduler.runScheduledCollection();

        assertThat(service.invocationCount).isZero();
    }

    @Test
    void delegatesToCollectionServiceWhenEnabled() {
        RecordingVkAutoCollectionService service = new RecordingVkAutoCollectionService();
        VkAutoCollectionScheduler scheduler = new VkAutoCollectionScheduler(
                new VkAutoCollectionProperties(true, "Primorsky Krai", 25, 10, 5, 20, "HYBRID", 900000L),
                service
        );

        scheduler.runScheduledCollection();

        assertThat(service.invocationCount).isEqualTo(1);
    }

    private static final class RecordingVkAutoCollectionService extends VkAutoCollectionService {
        private int invocationCount;

        private RecordingVkAutoCollectionService() {
            super(
                    new VkAutoCollectionProperties(true, "Primorsky Krai", 25, 10, 5, 20, "HYBRID", 900000L),
                    null,
                    null,
                    null
            );
        }

        @Override
        public void collect() {
            invocationCount++;
        }
    }
}
