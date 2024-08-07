/*
 * This file is part of SubTypo.
 *
 * SubTypo is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * SubTypo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SubTypo.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.teixeira.subtitles.managers;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.os.ParcelCompat;
import com.teixeira.subtitles.models.Subtitle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class manages the undo and redo stack.
 *
 * @author Felipe Teixeira
 * @since 2024-08-03
 */
public class UndoManager implements Parcelable {

  public static final Creator<UndoManager> CREATOR =
      new Creator<>() {

        @Override
        public UndoManager createFromParcel(Parcel parcel) {
          return new UndoManager(parcel);
        }

        @Override
        public UndoManager[] newArray(int size) {
          return new UndoManager[size];
        }
      };

  private List<StackItem> subtitlesStack;
  private boolean enabled;
  private int stackPointer;
  private int maxStackSize;

  /**
   * Creates a new UndoManager with the specified maximum stack size.
   *
   * @param maxStackSize The maximum size of undo and redo stacks.
   */
  public UndoManager(int maxStackSize) {
    this.subtitlesStack = new ArrayList<>();
    this.enabled = true;
    this.stackPointer = 0;
    this.maxStackSize = maxStackSize;
  }

  private UndoManager(Parcel parcel) {
    enabled = parcel.readBoolean();
    stackPointer = parcel.readInt();
    maxStackSize = parcel.readInt();
    subtitlesStack = new ArrayList<>();
    int count = parcel.readInt();
    for (int i = 0; i < count; i++) {
      subtitlesStack.add(
          ParcelCompat.readParcelable(parcel, UndoManager.class.getClassLoader(), StackItem.class));
    }
  }

  /**
   * Adds a list of subtitles to the stack and shifts the stack pointer to the new list.
   *
   * @param subtitles The list that will be added to the stack.
   */
  public void pushStack(List<Subtitle> subtitles) {

    if (!enabled) return;

    if (stackPointer < subtitlesStack.size()) {
      StackItem lastItem = subtitlesStack.get(stackPointer);
      if (lastItem != null && lastItem.equals(subtitles)) {
        return;
      }
    }

    while (stackPointer < subtitlesStack.size() - 1) {
      subtitlesStack.remove(subtitlesStack.size() - 1);
    }

    subtitlesStack.add(new StackItem(subtitles));
    if (subtitlesStack.size() == maxStackSize) {
      subtitlesStack.remove(0);
    }
    stackPointer = subtitlesStack.size() - 1;
  }

  /**
   * Checks if you can undo.
   *
   * @return Whether or not you can undo it.
   */
  public boolean canUndo() {
    return enabled && stackPointer > 0;
  }

  /**
   * Checks if you can redo.
   *
   * @return Whether or not you can redo it.
   */
  public boolean canRedo() {
    return enabled && stackPointer < subtitlesStack.size() - 1;
  }

  /**
   * Moves the stack pointer to the previous stack if you can undo.
   *
   * @return The previous stack list, or null if undo is not possible.
   */
  public List<Subtitle> undo() {
    if (canUndo()) {
      stackPointer--;
      return subtitlesStack.get(stackPointer).toList();
    }
    return null;
  }

  /**
   * Moves the stack pointer to the next stack if you can redo.
   *
   * @return The list of the next stack, or null if redo is not possible.
   */
  public List<Subtitle> redo() {
    if (canRedo()) {
      stackPointer++;
      return subtitlesStack.get(stackPointer).toList();
    }
    return null;
  }

  /**
   * @return Whether UndoManager is enabled.
   */
  public boolean isEnabled() {
    return this.enabled;
  }

  /**
   * Defines whether the UndoManager is enabled with the new specified value.
   *
   * @param enabled The boolean value that defines enabled/disabled.
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int flags) {
    parcel.writeBoolean(enabled);
    parcel.writeInt(stackPointer);
    parcel.writeInt(maxStackSize);
    parcel.writeInt(subtitlesStack.size());
    for (StackItem item : subtitlesStack) {
      parcel.writeParcelable(item, flags);
    }
  }

  /** This class is a model object that saves the list of subtitles from the stack. */
  public static class StackItem implements Parcelable {

    public static final Creator<StackItem> CREATOR =
        new Creator<>() {

          @Override
          public StackItem createFromParcel(Parcel parcel) {
            StackItem item = new StackItem();
            item.subtitles =
                (Subtitle[])
                    ParcelCompat.readArray(
                        parcel, StackItem.class.getClassLoader(), Subtitle.class);
            return item;
          }

          @Override
          public StackItem[] newArray(int size) {
            return new StackItem[size];
          }
        };

    Subtitle[] subtitles;

    StackItem() {}

    /**
     * Creates a new stack item and clones the subtitle list into an array.
     *
     * @param subtitleList The list of subtitles for this stack item.
     */
    StackItem(List<Subtitle> subtitleList) {
      this.subtitles = new Subtitle[subtitleList.size()];
      for (int i = 0; i < subtitleList.size(); i++) {
        this.subtitles[i] = subtitleList.get(i).clone();
      }
    }

    public List<Subtitle> toList() {
      List<Subtitle> subtitleList = new ArrayList<>();
      for (Subtitle subtitle : subtitles) {
        subtitleList.add(subtitle);
      }
      return subtitleList;
    }

    public boolean equals(List<Subtitle> subtitlesList) {
      if (subtitles.length != subtitlesList.size()) {
        return false;
      }

      return toList().equals(subtitlesList);
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
      parcel.writeArray(subtitles);
    }
  }
}
