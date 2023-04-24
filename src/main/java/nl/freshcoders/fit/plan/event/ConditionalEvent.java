package nl.freshcoders.fit.plan.event;

import nl.freshcoders.fit.plan.event.fault.Fault;
import nl.freshcoders.fit.plan.event.trigger.Trigger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ConditionalEvent {

    private String type;

    private Fault fault;

    private Trigger trigger;

    private boolean triggered = false;

    public String getType() {
        return trigger.getType();
    }

    public void setType(String type) {
        this.type = type;
    }

    public Fault getFault() {
        return fault;
    }

    public void setFault(Fault fault) {
        this.fault = fault;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public void trigger() {
        if (this.getFault().getOccurrence().getTiming().equals("once")) {
            this.triggered = true;
            // unload the plan from the agent(s)
        }
    }

    public Integer toHash() {
        try {
            String input = this.getFault().toString() + this.getTrigger().toString();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());

            // Convert the hash to an int value
            int hashInt = 0;
            for (int i = 0; i < 4; i++) {
                hashInt |= (hash[i] & 0xff) << (8 * i);
            }
            return hashInt;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void reset() {
        this.triggered = false;
    }
}
