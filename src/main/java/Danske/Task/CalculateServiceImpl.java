package Danske.Task;

import java.util.ArrayList;
import java.util.List;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

@Service
public class CalculateServiceImpl implements CalculateService {


    public List<SumAndIndex> calculateNodeValue(SumAndIndex currentNode, int val) {
        int reminder = val % 2;
        if (currentNode.indexWithReminder.reminder == reminder) {
            return ImmutableList.of();
        }
        List<SumAndIndex> result = new ArrayList<>();
        int sum = currentNode.sum + val;
        List<Integer> path = new ArrayList<>(currentNode.path);
        path.add(val);
        result.add(new SumAndIndex(currentNode.indexWithReminder.targetIndex, sum, reminder, path));
        result.add(new SumAndIndex(currentNode.indexWithReminder.targetIndex + 1, sum, reminder, path));
        return result;
    }

    @AutoProperty
    public static class SumAndIndex {
        private final IndexWithReminder indexWithReminder;
        private final int sum;
        private final List<Integer> path;

        public SumAndIndex(int targetIndex, int sum, int reminder, List<Integer> path) {
            this.indexWithReminder = new IndexWithReminder(targetIndex, reminder);
            this.sum = sum;
            this.path = path;
        }

        public IndexWithReminder getIndexWithReminder() {
            return indexWithReminder;
        }

        public int getSum() {
            return sum;
        }

        public List<Integer> getPath() {
            return path;
        }

        @Override public boolean equals(Object o) {
            return Pojomatic.equals(this, o);
        }

        @Override public int hashCode() {
            return Pojomatic.hashCode(this);
        }

        @Override public String toString() {
            return Pojomatic.toString(this);
        }
    }

    @AutoProperty
    public static class IndexWithReminder {
        private final int targetIndex;
        private final int reminder;

        public IndexWithReminder(int targetIndex, int reminder) {
            this.targetIndex = targetIndex;
            this.reminder = reminder;
        }

        public int getTargetIndex() {
            return targetIndex;
        }

        public int getReminder() {
            return reminder;
        }

        @Override public boolean equals(Object o) {
            return Pojomatic.equals(this, o);
        }

        @Override public int hashCode() {
            return Pojomatic.hashCode(this);
        }

        @Override public String toString() {
            return Pojomatic.toString(this);
        }
    }
}
