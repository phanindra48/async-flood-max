import matplotlib.pyplot as plt
from networkx import nx
import sys
import numpy as np

if __name__ == '__main__':
  if len(sys.argv) != 0:
    N = int(sys.argv[1])
    # G = nx.erdos_renyi_graph(N, 0.5, seed=None, directed=False)
    G = nx.erdos_renyi_graph(N, 0.1, seed=None, directed=False)
    # nx.draw(G)
    # plt.show()

    # A = nx.adjacency_matrix(G)

    matrix = nx.to_numpy_matrix(G)

    with open("../connectivity3.txt", "w+") as file:
      file.write(str(N))
      file.write('\n')
      file.write(' '.join([str(i + 1) for i in range(0, N)]))
      file.write('\n')
      counter = 0
      list = []
      for x in np.nditer(matrix):
        list.append(x)
        if len(list) == N:
          file.write(' '.join([str(int(i)) for i in list]))
          file.write('\n')
          list = []
