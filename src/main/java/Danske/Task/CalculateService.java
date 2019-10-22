package Danske.Task;

import java.util.List;

public interface CalculateService {

    List<CalculateServiceImpl.SumAndIndex> calculateNodeValue(CalculateServiceImpl.SumAndIndex currentNode, int val);
}
