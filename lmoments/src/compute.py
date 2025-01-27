import concurrent.futures
import json
import logging as log
import os
import pickle
import sys
import time
from datetime import datetime
from pathlib import Path
from queue import Queue

import numpy as np
import pandas as pd
from sklearn.ensemble import VotingClassifier, StackingClassifier
from sklearn.linear_model import SGDClassifier
from sklearn.metrics import balanced_accuracy_score
from sklearn.model_selection import train_test_split
from sklearn.neighbors import KNeighborsClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.svm import SVC
from sklearn.utils import resample

from lmoments import Lmoments

dirname = os.path.dirname(__file__)

# Default parameters of the project folders and the keys of the JSON files
conf_path = 'conf_default'
out_path = 'out'

main_keys = ['compute_lmom', 'n', 'parallel', 'plot_features', 'save_features']
dataset_keys = ['dataset', 'features', 'labels']

queue = Queue()


def compute_lmom(df, n, feature):
    """
    This function divides the dataframe to compute the L-moments and L-moment ratios with $n$ values.

    :param df: Dataframe with the dataset
    :param n: Number of values to compute L-moments and L-moment ratios
    :param feature: Flow feature to compute
    :return: L-moment and L-moment ratios for the flow $feature$
    """
    df = pickle.loads(df)

    if df.shape[0] % n == 0:
        size = df.shape[0] // n
    else:
        size = (df.shape[0] // n) + 1

    n_lmoments = 5

    tau3 = []
    tau4 = []
    tau5 = []

    for i in range(0, df.shape[0], n):
        if i + n < df.shape[0]:
            _lmom, _lmom_ratio = Lmoments(df.iloc[i:i + n - 1].to_numpy(), n_lmoments).compute()
        else:
            _lmom, _lmom_ratio = Lmoments(df.iloc[i:df.shape[0] - 1].to_numpy(), n_lmoments).compute()

        tau3.append(_lmom_ratio[3])
        tau4.append(_lmom_ratio[4])
        tau5.append(_lmom_ratio[5])

    queue.put({feature + ' (tau3)': tau3, feature + ' (tau4)': tau4, feature + ' (tau5)': tau5})


class Data:
    """
    Class that implements the functions needed to process the data.
    """

    def __init__(self, file, features, labels_column):
        self.file = file
        self.features = features
        self.labels_column = labels_column
        self.data = None
        self.labels = None

        self.extract_data()

    def extract_data(self):
        """
        Data extractor from the CSV files. Extract only the columns of the CSV files indicated by the user
        in the <dataset>.json file. In addition, extract the labels and split the data in train, valiaation
        and test sets.
        """

        log.info('Reading CSV...')
        self.data = pd.read_csv(os.path.join(dirname[:-4], 'data/', self.file), usecols=self.features + [self.labels_column], skipinitialspace=True)
        self.data.replace([np.inf, -np.inf], np.nan, inplace=True)
        self.data.dropna(inplace=True)
        self.labels = self.data.pop(self.labels_column)

        log.info('Splitted dataset in train, validation and test sets')

    @classmethod
    def get_labels_int(cls, l_mom_labels):
        """
        Class method that parses the labels from any data type to integer.
        :param l_mom_labels: Labels as any data type
        :return: Labels as integer data type
        """
        # Use a dictionary to preserve the order of first occurrence
        label_to_int = {}
        l_mom_labels_int = []

        for label in l_mom_labels:
            if label not in label_to_int:
                label_to_int[label] = len(label_to_int)
            l_mom_labels_int.append(label_to_int[label])

        return l_mom_labels_int


def get_labels(labels, n):
    """
    Function that returns the labels for each L-moment and L-moment ratios. For each value it assigns the label
    with higher contribution.

    :param labels: Labels for each flow
    :param n: Number of values used to calculate the L-moments and L-moment ratios
    :param labels_column: Labels column name
    :return:
    """

    return [labels.iloc[j:j + n].value_counts()[:1].index[0] for j in range(0, len(labels), n)]


def main(file):
    """
    Main function. Runs if the JSON confiuration files are verified.

    This function calls the data extraction class, plans the computations
    of the L-moments and calls the function in charge of performing
    the classification and obtaining the quality metrics.

    :param conf: Dict with the configuration variables
    """
    try:
        with open('lmoments/conf_processed/' + file.split('/')[-1]) as json_file:
            conf_file = json.load(json_file)
    except:
        print(file)
        exit()

    n = conf_file['n']
    data = Data(conf_file['dataset'], conf_file['features'], conf_file['labels'])
    features = [f"{feature} (tau3)" for feature in conf_file['features']] + [f"{feature} (tau4)" for feature in conf_file['features']] + [f"{feature} (tau5)" for feature in conf_file['features']]

    _file = file.split('/')[-1]
    lmom_csv = f'{conf_file["Data"]}/{_file[:-5]}.{conf_file["n"]}.csv'
    lock_file = f"{lmom_csv}.lock"

    start = time.time()

    if not Path(lmom_csv).exists():
        cpus = 16
        l_mom = {}
        features_to_compute = conf_file['features']

        with concurrent.futures.ThreadPoolExecutor(max_workers=cpus) as executor:
            futures = []
            for feature in features_to_compute:
                df_serialized = pickle.dumps(data.data[feature])
                futures.append(executor.submit(compute_lmom, df_serialized, n, feature))
            concurrent.futures.wait(futures)

            for future in futures:
                if future.exception() is not None:
                    print(sys.float_info.min)
                    exit(0)

            while not queue.empty():
                result = queue.get()
                l_mom.update(result)

        try:
            acquire_lock(lock_file)
            df = pd.DataFrame({col: l_mom.get(col, []) for col in features})
            df.to_csv(lmom_csv, index=False, header=True)
        finally:
            release_lock(lock_file)

    else:
        try:
            acquire_lock(lock_file)
            df = pd.read_csv(lmom_csv)
        finally:
            release_lock(lock_file)
    print(time.time() - start)

    x = df[conf_file['features_tau']]
    y = np.array(data.get_labels_int(get_labels(data.labels, n)))

    df_combined = pd.DataFrame(x, columns=conf_file['features_tau'])
    df_combined['label'] = y

    columns_all_zero = df_combined[conf_file['features_tau']].columns[(df_combined[conf_file['features_tau']] == 0.0).all(axis=0)].tolist()
    print(columns_all_zero)

    df_combined = df_combined.loc[~(df_combined[conf_file['features_tau']] == 0.0).all(axis=1)]
    # df_combined = df_combined.drop_duplicates(subset=conf_file['features_tau'] + ['label'])

    x = df_combined[conf_file['features_tau']].values
    y = df_combined['label'].values

    classification(x, y, conf_file)


def classification(x, y, conf_file, random_state=42):

    models = {
        'SGD Classifier': SGDClassifier(
            penalty=conf_file['SGD Classifier']['penalty'],
            loss=conf_file['SGD Classifier']['loss'],
            random_state=random_state),
        'SVC RBF': SVC(
            kernel='rbf',
            gamma=conf_file['SVC RBF']['gamma'],
            random_state=random_state),
        'SVC poly': SVC(
            kernel='poly',
            degree=conf_file['SVC poly']['degree'],
            gamma=conf_file['SVC poly']['gamma'],
            random_state=random_state),
        'KNeighborsClassifier': KNeighborsClassifier(
            n_neighbors=conf_file['KNN']['n_neighbors'],
            weights=conf_file['KNN']['weight'])
    }

    estimators = [(name, model) for name, model in models.items()]

    if conf_file['ensemble'] == 0:
        classifier = VotingClassifier(estimators=estimators, voting='hard', n_jobs=16)
    else:
        classifier = StackingClassifier(estimators=estimators, n_jobs=16)

    scaler = StandardScaler()
    x = scaler.fit_transform(x)

    # Cross-validation
    cv = StratifiedShuffleSplit(n_splits=4, test_size=0.2, random_state=random_state)

    balanced_accuracies = []

    for train_idx, test_idx in cv.split(x, y):
        x_train, x_test = x[train_idx], x[test_idx]
        y_train, y_test = y[train_idx], y[test_idx]

        # Fit clasifier and predict
        classifier.fit(x_train, y_train)
        y_pred = classifier.predict(x_test)

        balanced_acc = balanced_accuracy_score(y_test, y_pred)
        balanced_accuracies.append(balanced_acc)

    mean_balanced_acc = np.mean(balanced_accuracies)

    print(mean_balanced_acc)


def acquire_lock(lock_file, timeout=240, wait_interval=2):
    """
    Intenta adquirir un archivo de bloqueo. Espera si ya existe.

    :param lock_file: Ruta del archivo de bloqueo.
    :param timeout: Tiempo máximo para esperar el archivo de bloqueo.
    :param wait_interval: Intervalo de espera entre intentos (en segundos).
    """
    if os.path.exists(lock_file) and is_stale_lock(lock_file):
        os.remove(lock_file)

    start_time = time.time()
    while os.path.exists(lock_file):
        if time.time() - start_time > timeout:
            raise TimeoutError(f"Timeout: no se pudo adquirir el archivo de bloqueo {lock_file}")
        time.sleep(wait_interval)
    with open(lock_file, 'w') as f:
        f.write(f"Bloqueado por PID: {os.getpid()}\n")


def release_lock(lock_file):
    """
    Libera el archivo de bloqueo.

    :param lock_file: Ruta del archivo de bloqueo.
    """
    try:
        if os.path.exists(lock_file):
            os.remove(lock_file)
    except Exception as _:
        pass


def is_stale_lock(lock_file, max_age=250):
    """
    Comprueba si un archivo de bloqueo es demasiado antiguo.

    :param lock_file: Ruta del archivo de bloqueo.
    :param max_age: Edad máxima permitida en segundos.
    :return: True si el archivo de bloqueo es antiguo, False de lo contrario.
    """
    if not os.path.exists(lock_file):
        return False
    lock_age = time.time() - os.path.getmtime(lock_file)
    return lock_age > max_age
