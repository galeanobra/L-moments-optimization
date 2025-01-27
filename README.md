[![Binder](https://mybinder.org/badge_logo.svg)](https://mybinder.org/v2/gh/galeanobra/L-moments-optimization/HEAD?urlpath=lab/tree/plots.ipynb) [![Replay](https://img.shields.io/badge/launch-EGI%20Replay-F5A252.svg)](https://replay.notebooks.egi.eu/v2/gh/galeanobra/L-moments-optimization/HEAD?urlpath=lab/tree/plots.ipynb)

This repository contains the raw results, framework code, and analysis scripts for the article **"Network traffic classification through high-order L-moments and multi-objective optimization"**, published in the journal *Journal*.

## Contents

- **Results Data:**  
  - Includes the results obtained after the optimization of all analyzed scenarios, from (a) to (e).
  - These results represent the sample size *n*, the amount of features selected, and balanced accuracy in `FUN.csv` files, as well as decision variables in `VAR.csv` files.

- **Jupyter Notebook:**
  - Contains the scripts used to generate the figures included in the main article.
  - Provides additional interactive visualizations, such as dynamic 3D plots, to enhance the analysis of the results.

- **Optimizer and L-moments code:**
  - The `src` folder contains the Java source code of the distributed optimizer.
  - The `lmoments` folder includes Python scripts for L-moment estimation.

## Usage

### Jupyter Notebook

We recommend running the notebook directly in the cloud using [mybinder](https://mybinder.org/v2/gh/galeanobra/L-moments-optimization/HEAD?urlpath=lab/tree/plots.ipynb) or [EGI Replay](https://replay.notebooks.egi.eu/v2/gh/galeanobra/L-moments-optimization/HEAD?urlpath=lab/tree/plots.ipynb). Note that the execution in the cloud may take a few minutes. But you can run it locally instead by following these steps:

1. Clone the repository:
   ```bash
   git clone https://github.com/galeanobra/L-moments-optimization.git
   cd L-moments-optimization
   ```

2. Ensure you have `pip` installed:
   - On most systems, `pip` comes pre-installed with Python. You can check if `pip` is installed by running:
     ```bash
     pip --version
     ```
   - If `pip` is not installed, you can install it by following the [official instructions](https://pip.pypa.io/en/stable/installation/).

3. Create a virtual environment (optional but recommended):
   ```bash
   python -m venv lmomvenv
   source lmomvenv/bin/activate  # On Windows use: lmomvenv\Scripts\activate
   ```

4. Install the dependencies in `requirements.txt` using `pip`:
   ```bash
   pip install -r requirements.txt
   ```

5. Open the Jupyter Notebook:
   - Use a Jupyter environment to open the `.ipynb` file provided in the repository:
     ```bash
     jupyter lab
     ```

6. Generate Figures:
   - Run the notebook to produce the static figures included in the article.
   - Explore interactive visualizations for deeper insights into the optimization process.

### Optimizer

To use the optimizer, ensure you have Java 17 or higher installed. You can execute the optimizer using the following command:

```bash
java -cp lmom-optimization.jar NSGAIIMain <server_port> <pop_size> <max_evals> <scenario_json>
```

Where:
- `<server_port>` is the port of the node running the algorithm.
- `<pop_size>` is the population size.
- `<max_evals>` is the stopping condition.
- `<scenario_json>` is a JSON file located in the `lmoments/conf_default` folder, corresponding to the predefined scenarios.

You can run as many worker nodes as desired with the following command:

```bash
java -cp lmom-optimization.jar Worker <IP_server> <port_server>
```

## Citation

If you use this repository in your work, please cite the original article:

```
@article{galeano2025network,
  title = {Network traffic classification through high-order L-moments and multi-objective optimization},
  author = {Galeano-Brajones, Jes{\'u}s and Chidean, Mihaela I and Luna, Francisco and Calle-Cancho, Jes{\'u}s and Carmona-Murillo, Javier},
  journal = {Preprint},
  year = {2025},
  volume = {XX},
  pages = {XX--XX},
  doi = {XX.XXXX/j.xx.xxxx},
}
```

## License

The contents of this repository, including code, data, and results, are provided solely for academic and research purposes. Use of the materials requires proper citation of the original article. Any commercial use, redistribution, or modification without explicit permission from the authors is strictly prohibited.

---

For any questions or further clarifications, please contact the authors.