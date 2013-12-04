/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dma.ais.coverage.calculator;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.coverage.AisCoverage;
import dk.dma.ais.coverage.data.CustomMessage;
import dk.dma.ais.coverage.data.Source;
import dk.dma.ais.coverage.data.Station;
import dk.dma.ais.coverage.event.AisEvent;
import dk.dma.ais.coverage.event.AisEvent.Event;
import dk.dma.ais.coverage.event.IAisEventListener;

/**
 * This calculator requires an unfiltered ais stream (no doublet filtering). It increments "received cell-message counter" for
 * corresponding sources, based on the messages that the supersource approves.
 */
public class DistributeOnlyCalculator extends AbstractCalculator implements IAisEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(AisCoverage.class);
    private static final long serialVersionUID = -528305318453243556L;

    // private long messagesProcessed = 0;
    // private Date start = new Date();
    private LinkedHashMap<String, Map<String, CustomMessage>> receivedMessages = new LinkedHashMap<String, Map<String, CustomMessage>>() {
        private static final long serialVersionUID = -8805956769136748240L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Map<String, CustomMessage>> eldest) {
            ((Map<String, CustomMessage>) eldest.getValue()).clear(); // seems to be necessary in order to keep application from
                                                                      // performance degration.
            return this.size() > 200000;
        }
    };

    public DistributeOnlyCalculator(boolean ignoreRotation, HashMap<String, Station> map) {
        super(map);
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    // Date now = new Date();
                    // int elapsed = (int) ((now.getTime() - start.getTime()) / 1000);
                    // LOG.debug("messages/sec: "+ messagesProcessed/elapsed+ "... received messages "+receivedMessages.size());
                }
            }
        });
        t1.start();
    }

    /*
     * Takes supersource message as input. Finds matching messages for each source and increments corresponding cells.
     */
    private void approveMessage(CustomMessage aprrovedMessage) {
        String key = aprrovedMessage.getKey();
        if (receivedMessages.containsKey(key)) {
            Map<String, CustomMessage> approvedMessages = receivedMessages.get(key);
            for (CustomMessage customMessage : approvedMessages.values()) {
                // increment cell in each source
                Source source = dataHandler.getSource(customMessage.getSourceMMSI());

                // Cell cell = dataHandler.getCell(source.getIdentifier(), customMessage.getLatitude(),
                // customMessage.getLongitude());
                // if (cell == null) {
                // cell = dataHandler.createCell(source.getIdentifier(), customMessage.getLatitude(), customMessage.getLongitude());
                // }

                dataHandler.getSource(source.getIdentifier()).incrementMessageCount();
                dataHandler.incrementReceivedSignals(source.getIdentifier(), customMessage.getLatitude(),
                        customMessage.getLongitude(), aprrovedMessage.getTimestamp());
                // cell.incrementNOofReceivedSignals();
                // dataHandler.updateCell(cell);
            }

            // Done processing - remove messages
            receivedMessages.remove(key);
        } else {
            LOG.error("Supersource approved a message, but it was not found in any sources " + key);
        }
    }

    /*
     * When supersource approves a message, we need to find all sources that received the message and increment
     * "received message counter" for corresponding cell in each source.
     */
    @Override
    public void aisEventReceived(AisEvent event) {
        if (event.getEvent() == Event.AISMESSAGE_APPROVED) {
            CustomMessage m = (CustomMessage) event.getEventObject();
            approveMessage(m);

        } else if (event.getEvent() == Event.AISMESSAGE_REJECTED) {
            CustomMessage m = (CustomMessage) event.getEventObject();
            rejectMessage(m);
        }
    }

    private void rejectMessage(CustomMessage m) {
        Map<String, CustomMessage> map = receivedMessages.remove(m);
        if (map != null) {
            map.clear();
        }

    }

    /*
     * For testing purposes
     */
    // private void printMessage(CustomMessage m) {
    // AisMessage aisM = m.getOriginalMessage();
    // try {
    // System.out.println(aisM.getEncoded().encode());
    // } catch (SixbitException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // System.out.println(m.getOriginalMessage());
    // System.out.println(m.getOriginalMessage().getClass());
    // System.out.println(m.getCog());
    // System.out.println(m.getLatitude());
    // System.out.println(m.getLongitude());
    // System.out.println(m.getShipMMSI());
    // System.out.println(m.getSog());
    // System.out.println(m.getTimestamp().getTime());
    // System.out.println(aisM.getSourceTag().getBaseMmsi());
    // System.out.println(messageToKey(m));
    //
    // System.out.println();
    // }

    @Override
    public void calculate(CustomMessage m) {

        // messagesProcessed++;
        Map<String, CustomMessage> list;
        String key = m.getKey();
        if (receivedMessages.containsKey(key)) {
            list = receivedMessages.get(key);
        } else {
            list = new HashMap<String, CustomMessage>();
            receivedMessages.put(key, list);
        }

        // we use a map, to filter doublets from a single source
        // Apparently, a ship sometimes send the same (apparently) message multiple times
        // within very little time... (smaller than the expected frequency).
        list.put(m.getSourceMMSI(), m);
    }
}