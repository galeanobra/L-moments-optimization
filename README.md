[![Binder](https://mybinder.org/badge_logo.svg)](https://mybinder.org/v2/gh/galeanobra/L-moments-optimization/HEAD?urlpath=lab/tree/plots.ipynb) [![Replay](https://img.shields.io/badge/launch-EGI%20Replay-F5A252.svg)](https://replay.notebooks.egi.eu/v2/gh/galeanobra/L-moments-optimization/HEAD?urlpath=lab/tree/plots.ipynb)

This repository contains the raw results, framework code, and analysis scripts for the article **\"Network traffic classification through high-order L-moments and multi-objective optimization\"**, published in the journal *Journal*.

## Contents

- **Results Data:**  
  - Includes the results obtained after the optimization of all analyzed scenarios, from (a) to (e).
  - These results represent the sample size *n*, the amount of features selected, and balanced accuracy in `FUN.csv` files, as well as decision variables in `VAR.csv` files.
  - The [`permutation_importance`](./permutation_importance) folder includes a Python script to compute the permutation importance and the files with the results obtained for each scenario.

- **Jupyter Notebook:**
  - Contains the scripts used to generate the figures included in the main article.
  - Provides additional interactive visualizations, such as dynamic 3D plots, to enhance the analysis of the results.

- **Optimizer and L-moments code:**
  - The [`src`](./src) folder contains the Java source code of the distributed optimizer.
  - The [`lmoments`](./lmoments) folder includes Python scripts for L-moment estimation.

## Dependencies, software requirements, and usage

This repository provides detailed dependency management to ensure reproducibility and facilitate broader adoption. Please ensure your environment matches the following specifications:

### Java Optimizer Framework
- **Java Development Kit (JDK)**: version 17 (or higher)
- **JMetal Framework** ([version 6.6](https://github.com/jMetal/jMetal/tree/jmetal-6.6)), managed through Maven with dependencies explicitly listed in the provided [`pom.xml`](./pom.xml) file, including:
  - `jmetal-core`
  - `jmetal-algorithm`
  - `jmetal-parallel`

You can compile the Java code easily using Maven:
```bash
mvn clean package
```

### Python environment (Jupyter Notebook and L-moments scripts)

We recommend running the notebook directly in the cloud using [mybinder](https://mybinder.org/v2/gh/galeanobra/L-moments-optimization/HEAD?urlpath=lab/tree/plots.ipynb) or [EGI Replay](https://replay.notebooks.egi.eu/v2/gh/galeanobra/L-moments-optimization/HEAD?urlpath=lab/tree/plots.ipynb). Note that cloud execution may take a few minutes. To run it locally or use Python scripts:

1. Clone the repository:
   ```bash
   git clone https://github.com/galeanobra/L-moments-optimization.git
   cd L-moments-optimization
   ```

2. Ensure Python (version 3.8 or higher) and `pip` are installed:
   - Check Python version:
     ```bash
     python --version
     ```
   - Check if `pip` is installed:
     ```bash
     pip --version
     ```
   - If not installed, follow [official Python installation instructions](https://www.python.org/downloads/) and [pip installation guide](https://pip.pypa.io/en/stable/installation/).

3. Create a virtual environment (optional but recommended):
   ```bash
   python -m venv lmomvenv
   source lmomvenv/bin/activate  # On Windows use: lmomvenv\Scripts\activate
   ```

4. Install Python dependencies listed in `requirements.txt`:
   ```bash
   pip install -r requirements.txt
   ```

5. Open the Jupyter Notebook:
   ```bash
   jupyter lab
   ```

6. Generate figures and perform analysis:
   - Run the notebook to produce static figures included in the article.
   - Explore interactive visualizations for deeper insights into the optimization process.
   - Use Python scripts in the [`lmoments`](./lmoments) folder to compute L-moments as needed.

### Optimizer execution

To use the optimizer, ensure you have Java 17 or higher installed. Execute the optimizer using:

```bash
java -cp lmom-optimization.jar NSGAIIMain <server_port> <pop_size> <max_evals> <scenario_json>
```

Where:
- `<server_port>`: Port of the node running the algorithm.
- `<pop_size>`: Population size.
- `<max_evals>`: Stopping condition.
- `<scenario_json>`: JSON file in `lmoments/conf_default` folder corresponding to predefined scenarios.

Run as many worker nodes as desired:

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
