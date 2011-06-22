/* 
Copyright Paul James Mutton, 2001-2009, http://www.jibble.org/

This file is part of PircBot.

This software is dual-licensed, allowing you to choose between the GNU
General Public License (GPL) and the www.jibble.org Commercial License.
Since the GPL may be too restrictive for use in a proprietary application,
a commercial license is also provided. Full license information can be
found at http://www.jibble.org/licenses/

*/


package org.jibble.pircbot;

import java.util.*;

/**
 * This class is used to process DCC events from the server.
 *
 * @since   1.2.0
 * @author  Paul James Mutton,
 *          <a href="http://www.jibble.org/">http://www.jibble.org/</a>
 * @version    1.5.0 (Build time: Mon Dec 14 20:07:17 2009)
 */
public class DccManager {
    
    
    /**
     * Constructs a DccManager to look after all DCC SEND and CHAT events.
     * 
     * @param bot The PircBot whose DCC events this class will handle.
     */
    DccManager(PircBot bot) {
        _bot = bot;
    }
    
    
    /**
     * Processes a DCC request.
     * 
     * @return True if the type of request was handled successfully.
     */
    boolean processRequest(String nick, String login, String hostname, String request) {
        StringTokenizer tokenizer = new StringTokenizer(request);
        tokenizer.nextToken();
        String type = tokenizer.nextToken();
        String filename = tokenizer.nextToken();
        
        if (type.equals("SEND")) {
            long address = Long.parseLong(tokenizer.nextToken());
            int port = Integer.parseInt(tokenizer.nextToken());
            long size = -1;
            try {
                size = Long.parseLong(tokenizer.nextToken());
            }
            catch (Exception e) {
                // Stick with the old value.
            }
            
            DccFileTransfer transfer = new DccFileTransfer(_bot, this, nick, login, hostname, type, filename, address, port, size);
            _bot.onIncomingFileTransfer(transfer);
            
        }
        else if (type.equals("RESUME")) {
            int port = Integer.parseInt(tokenizer.nextToken());
            long progress = Long.parseLong(tokenizer.nextToken());
            
            DccFileTransfer transfer = null;
            synchronized (_awaitingResume) {
                for (int i = 0; i < _awaitingResume.size(); i++) {
                    transfer = (DccFileTransfer) _awaitingResume.elementAt(i);
                    if (transfer.getNick().equals(nick) && transfer.getPort() == port) {
                        _awaitingResume.removeElementAt(i);
                        break;
                    }
                }
            }
            
            if (transfer != null) {
                transfer.setProgress(progress);
                _bot.sendCTCPCommand(nick, "DCC ACCEPT file.ext " + port + " " + progress);
            }
            
        }
        else if (type.equals("ACCEPT")) {
            int port = Integer.parseInt(tokenizer.nextToken());
            long progress = Long.parseLong(tokenizer.nextToken());
            
            DccFileTransfer transfer = null;
            synchronized (_awaitingResume) {
                for (int i = 0; i < _awaitingResume.size(); i++) {
                    transfer = (DccFileTransfer) _awaitingResume.elementAt(i);
                    if (transfer.getNick().equals(nick) && transfer.getPort() == port) {
                        _awaitingResume.removeElementAt(i);
                        break;
                    }
                }
            }
            
            if (transfer != null) {
                transfer.doReceive(transfer.getFile(), true);
            }
            
        }
        else if (type.equals("CHAT")) {
            long address = Long.parseLong(tokenizer.nextToken());
            int port = Integer.parseInt(tokenizer.nextToken());
            
            final DccChat chat = new DccChat(_bot, nick, login, hostname, address, port);
            
            new Thread() {
                public void run() {
                    _bot.onIncomingChatRequest(chat);
                }
            }.start();
        }
        else {
            return false;
        }
        
        return true;
    }
    
    
    /**
     * Add this DccFileTransfer to the list of those awaiting possible
     * resuming.
     * 
     * @param transfer the DccFileTransfer that may be resumed.
     */
    void addAwaitingResume(DccFileTransfer transfer) {
        synchronized (_awaitingResume) {
            _awaitingResume.addElement(transfer);
        }
    }
    
    
    /**
     * Remove this transfer from the list of those awaiting resuming.
     */
    void removeAwaitingResume(DccFileTransfer transfer) {
        _awaitingResume.removeElement(transfer);
    }
    
    
    private PircBot _bot;
    private Vector _awaitingResume = new Vector();
    
}
