package com.stericson.RootTools.internal;

import com.stericson.RootTools.execution.Command;

class InternalBase {

    protected class InternalCommand extends Command {

        public InternalCommand(int id, String... command) {
            super(id, command);
        }

        @Override
        public void output(int id, String line) {
            //does nothing
        }

        @Override
        public void commandTerminated(int id, String reason) {
            synchronized (InternalBase.this) {
                InternalBase.this.notifyAll();
            }
        }

        @Override
        public void commandCompleted(int id, int exitCode) {
            synchronized (InternalBase.this) {
                InternalBase.this.notifyAll();
            }
        }
    }

    protected void commandWait() {
        synchronized (InternalBase.this) {
            try {
                InternalBase.this.wait();
            } catch (InterruptedException e) {}
        }
    }
}
