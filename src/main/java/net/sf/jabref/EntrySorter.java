/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref;

import java.util.*;

public class EntrySorter implements DatabaseChangeListener {

    //TreeSet set;
    private final ArrayList<BibtexEntry> set;
    private final Comparator<BibtexEntry> comp;
    private String[] idArray;
    private BibtexEntry[] entryArray;
    private boolean changed = false;


    public EntrySorter(Map<String, BibtexEntry> entries, Comparator<BibtexEntry> comp) {
        //set = new TreeSet(comp);
        set = new ArrayList<BibtexEntry>();
        this.comp = comp;
        Set<String> keySet = entries.keySet();
        for (Map.Entry<String, BibtexEntry> stringBibtexEntryEntry : entries.entrySet()) {
            set.add(stringBibtexEntryEntry.getValue());
        }
        //Collections.sort(set, comp);
        changed = true;
        index();
    }

    private void index() {

        /*  Old version, from when set was a TreeSet.

        // The boolean "changing" is true in the situation that an entry is about to change,
        // and has temporarily been removed from the entry set in this sorter. So, if we index
        // now, we will cause exceptions other places because one entry has been left out of
        // the indexed array. Simply waiting foth this to change can lead to deadlocks,
        // so we have no other choice than to return without indexing.
        if (changing)
            return;
        */

        synchronized (set) {

            // Resort if necessary:
            if (changed) {
                Collections.sort(set, comp);
                changed = false;
            }

            // Create an array of IDs for quick access, since getIdAt() is called by
            // getValueAt() in EntryTableModel, which *has* to be efficient.

            int count = set.size();
            idArray = new String[count];
            entryArray = new BibtexEntry[count];
            int piv = 0;
            for (BibtexEntry entry : set) {
                //        for (int i=0; i<idArray.length; i++) {
                idArray[piv] = entry.getId();
                entryArray[piv] = entry;
                piv++;
            }
        }
    }

    public boolean isOutdated() {
        return false;
    }

    public String getIdAt(int pos) {
        synchronized (set) {
            return idArray[pos];
        }
        //return ((BibtexEntry)(entryArray[pos])).getId();
    }

    public BibtexEntry getEntryAt(int pos) {
        synchronized (set) {
            return entryArray[pos];
        }
    }

    public int getEntryCount() {
        synchronized (set) {
            if (entryArray != null) {
                return entryArray.length;
            } else {
                return 0;
            }
        }
    }

    @Override
    public void databaseChanged(DatabaseChangeEvent e) {
        synchronized (set) {
            int pos;
            switch (e.getType()) {
            case ADDED_ENTRY:
                pos = -Collections.binarySearch(set, e.getEntry(), comp) - 1;
                set.add(pos, e.getEntry());
                //addEntry(e.getEntry());
                //set.add(e.getEntry());
                //changed = true;
                //Collections.sort(set, comp);
                break;
            case REMOVED_ENTRY:
                set.remove(e.getEntry());
                changed = true;
                break;
            case CHANGED_ENTRY:
                // Entry changed. Resort list:
                //Collections.sort(set, comp);
                pos = Collections.binarySearch(set, e.getEntry(), comp);
                int posOld = set.indexOf(e.getEntry());
                if (pos < 0) {
                    set.remove(posOld);
                    set.add(-pos - 1, e.getEntry());
                }
                //changed = true;
                break;
            }

        }

    }
}