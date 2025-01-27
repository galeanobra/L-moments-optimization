import matplotlib.pyplot as plt
import numpy as np
from scipy.special import comb


class Lmoments:
    """
    Class that implements the L-moments computation algorithm. For a sample $x$ of size $n$, this algorithm
    computes the L-moments and the L-moment ratios until the order `nl`.
    """

    def __init__(self, x, nl=5):
        """
        Default constructor. Receives the sample and the maximum order for L-moments computation.

        :param x: sample
        :param nl: Order of the last L-moment computed.
        """
        self.x = np.sort(x)  # Convert sample to a NumPy array
        self.n = len(x)
        self.nl = nl
        self.comb_matrix = self._precompute_comb_matrix(self.n, self.nl)

    def _precompute_comb_matrix(self, n, nl):
        """
        Precompute the combination matrix for the sample size.

        :param n: Number of elements in the sample.
        :param nl: Maximum order of combinations.
        :return: A precomputed combination matrix as a NumPy array.
        """
        comb_matrix = np.zeros((n, nl), dtype=int)
        for r in range(nl):
            comb_matrix[:, r] = [comb(i, r, exact=True) for i in range(n)]
        return comb_matrix

    def _calculate_weights(self, r):
        """
        Generalized calculation of weights for L-moments.

        :param r: Order of the L-moment.
        :return: A NumPy array of weights.
        """
        weights = np.zeros(self.n)
        for k in range(0, r):
            coeff = comb(r - 1, k, exact=True) * ((-1) ** k)
            weights += coeff * self.comb_matrix[:, r - k - 1] * self.comb_matrix[::-1, k]
        return weights

    def compute(self):
        """
        Compute the L-moments and L-moment ratios for the sample `x`.

        :return: tuple (<L-moments>, <L-moment ratios>), both as dictionaries.
        """
        lmoments = {}
        lmoment_ratio = {}

        # First order (mean)
        lmoments[1] = np.mean(self.x)

        if self.nl > 1:
            for r in range(2, self.nl + 1):
                denom = comb(self.n, r, exact=True)  # Binomial coefficient for denominator
                if denom > 0:
                    weights = self._calculate_weights(r)
                    lmoments[r] = (1 / r / denom) * np.sum(weights * self.x)
                else:
                    lmoments[r] = 0

        # L-moment ratios
        if self.nl > 2:
            for r in range(3, self.nl + 1):
                lmoment_ratio[r] = lmoments[r] / lmoments[2] if lmoments[2] != 0 else 0

        return lmoments, lmoment_ratio

    def comb(self, n, k):
        """
        Compute the binomial coefficient C(n, k) using scipy's comb function.

        :param n: Total number of items.
        :param k: Number of items to choose.
        :return: Binomial coefficient.
        """
        return comb(n, k, exact=True)

    @staticmethod
    def plot_lmoments(df, columns, label_column=None):
        """
        Plot the L-moment diagram with 1, 2, or 3 columns from a DataFrame.

        :param df: Pandas DataFrame containing the data.
        :param columns: List of column names (1 to 3) to plot.
        :param label_column: Optional. Column name for point labels/colors.
        """
        if len(columns) not in [1, 2, 3]:
            raise ValueError("You must provide 1, 2, or 3 columns to plot.")

        data = df[columns].to_numpy()
        labels = None
        if label_column:
            labels = df[label_column].to_numpy()
            unique_labels = np.unique(labels)
            colors = plt.rcParams['axes.prop_cycle'].by_key()['color']
            color_map = {label: colors[i % len(colors)] for i, label in enumerate(unique_labels)}
            point_colors = [color_map[label] for label in labels]
        else:
            point_colors = 'blue'

        if len(columns) == 1:
            plt.figure(figsize=(8, 6))
            if label_column:
                for label, color in color_map.items():
                    mask = labels == label
                    plt.scatter(range(len(data[mask])), data[mask], label=str(label), color=color)
            else:
                plt.plot(range(len(data)), data, color=point_colors, marker='o')
            plt.xlabel("Index")
            plt.ylabel(columns[0])
            plt.grid(True)
            if label_column:
                plt.legend()
            plt.savefig('prueba.png', dpi=300)
        elif len(columns) == 2:
            plt.figure(figsize=(8, 6))
            if label_column:
                for label, color in color_map.items():
                    mask = labels == label
                    plt.scatter(data[mask, 0], data[mask, 1], label=str(label), color=color)
            else:
                plt.scatter(data[:, 0], data[:, 1], color=point_colors)
            plt.xlabel(columns[0])
            plt.ylabel(columns[1])
            plt.grid(True)
            if label_column:
                plt.legend()
            plt.show()
        else:
            fig = plt.figure(figsize=(10, 8))
            ax = fig.add_subplot(111, projection='3d')
            if label_column:
                for label, color in color_map.items():
                    mask = labels == label
                    ax.scatter(data[mask, 0], data[mask, 1], data[mask, 2], label=str(label), color=color)
            else:
                ax.scatter(data[:, 0], data[:, 1], data[:, 2], color=point_colors)
            ax.set_xlabel(columns[0])
            ax.set_ylabel(columns[1])
            ax.set_zlabel(columns[2])
            if label_column:
                ax.legend()
            plt.show()

