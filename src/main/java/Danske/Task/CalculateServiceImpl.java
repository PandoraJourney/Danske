package Danske.Task;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

@Service
public class CalculateServiceImpl implements CalculateService {


    public List<SumAndIndex> calculateNodeValue(SumAndIndex currentNode, int val) {
        int reminder = val % 2;
        if (currentNode.reminder == reminder) {
            return ImmutableList.of();
        }
        List<SumAndIndex> result = new ArrayList<>();
        int sum = currentNode.sum + val;
        List<Integer> path = new ArrayList<>(currentNode.path);
        path.add(val);
        result.add(new SumAndIndex(currentNode.targetIndex, sum, reminder, path));
        result.add(new SumAndIndex(currentNode.targetIndex + 1, sum, reminder, path));
        return result;
    }

    public static class SumAndIndex {
        private final int targetIndex;
        private final int sum;
        private final int reminder;
        private final List<Integer> path;

        public SumAndIndex(int targetIndex, int sum, int reminder, List<Integer> path) {
            this.targetIndex = targetIndex;
            this.sum = sum;
            this.reminder = reminder;
            this.path = path;
        }

        public int getTargetIndex() {
            return targetIndex;
        }

        public int getSum() {
            return sum;
        }

        public List<Integer> getPath() {
            return path;
        }
    }
}
