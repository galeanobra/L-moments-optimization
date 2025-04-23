import json
import os
from collections import defaultdict

import numpy as np
import pandas as pd
from sklearn.ensemble import VotingClassifier, StackingClassifier
from sklearn.inspection import permutation_importance
from sklearn.linear_model import SGDClassifier, LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.neighbors import KNeighborsClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.svm import SVC

from compute import Data, get_labels


def load_config(base_file):
    if not os.path.isfile(base_file):
        raise FileNotFoundError(f"Configuration file '{base_file}' not found.")
    with open(base_file, 'r') as f:
        return json.load(f)


def compute_importance(x_train, x_test, y_train, y_test, classifier, features_tau):
    classifier.fit(x_train, y_train)
    result = permutation_importance(
        classifier, x_test, y_test,
        scoring='balanced_accuracy',
        n_repeats=3,
        random_state=42,
        n_jobs=-1
    )
    return [(features_tau[i], result.importances_mean[i]) for i in range(len(features_tau))]


if __name__ == '__main__':
    var_path = '../(a) NTP/VAR.csv'  # Scenario
    with open(var_path, 'r') as f:
        lines = [line.strip() for line in f.readlines() if line.strip()]

    assert len(lines) % 3 == 0, "VAR.csv does not have a correct number of lines"
    num_solutions = len(lines) // 3

    importance_sums = defaultdict(list)

    for sol_id in range(num_solutions):
        print(f'\n==> Solution {sol_id}')
        try:
            feature_bits = lines[sol_id * 3].split('Variables:')[1].split('Objectives:')[0].strip()
            ensemble_bits = [int(c) for c in lines[sol_id * 3 + 1].split('Variables:')[1].split('Objectives:')[0].strip()]
            integers = list(map(int, lines[sol_id * 3 + 2].split('Variables:')[1].split('Objectives:')[0].strip().split()))
        except Exception as e:
            print(f'  > Error processing format: {e}')
            continue

        try:
            base_file = '../lmoments/conf_default/CIC-DDoS2019-01-12_NTP.json'  # The dataset is needed
            config = load_config(base_file)

            list_features_tau = []
            for i in range(len(feature_bits)):
                if feature_bits[i] == '1':
                    base_feature = config.get('features', [])[i // 3]
                    if i % 3 == 0:
                        fname = f'{base_feature} (tau3)'
                    elif i % 3 == 1:
                        fname = f'{base_feature} (tau4)'
                    else:
                        fname = f'{base_feature} (tau5)'
                    list_features_tau.append(fname)

            config.update({
                'features_tau': list_features_tau,
                'n': integers[0],
                'ensemble': ensemble_bits[0],
                'SGD Classifier': {
                    'penalty': 'l1' if ensemble_bits[1] == 0 else 'l2',
                    'loss': 'squared_hinge' if ensemble_bits[2] == 1 or ensemble_bits[1] == 0 else 'hinge'
                },
                'SVC RBF': {'gamma': 'scale' if ensemble_bits[3] == 0 else 'auto'},
                'SVC poly': {'degree': integers[1], 'gamma': 'scale' if ensemble_bits[4] == 0 else 'auto'},
                'KNN': {'n_neighbors': integers[2], 'weight': 'uniform' if ensemble_bits[5] == 0 else 'distance'}
            })

            conf_file = config
            n = conf_file['n']
            data = Data(conf_file['dataset'], conf_file['features'], conf_file['labels'])

            _file = base_file.split('/')[-1]
            lmom_csv = f'{conf_file["Data"]}/{_file[:-5]}.{conf_file["n"]}.csv'

            df = pd.read_csv(lmom_csv)
            x = df[conf_file['features_tau']]
            y = np.array(data.get_labels_int(get_labels(data.labels, n)))

            scaler = StandardScaler()
            x_scaled = scaler.fit_transform(x)
            x_train, x_test, y_train, y_test = train_test_split(x_scaled, y, test_size=0.2, stratify=y, random_state=42)

            models = {
                'SGD Classifier': SGDClassifier(
                    penalty=conf_file['SGD Classifier']['penalty'],
                    loss=conf_file['SGD Classifier']['loss'],
                    random_state=42),
                'SVC RBF': SVC(
                    kernel='rbf',
                    gamma=conf_file['SVC RBF']['gamma'],
                    random_state=42),
                'SVC poly': SVC(
                    kernel='poly',
                    degree=conf_file['SVC poly']['degree'],
                    gamma=conf_file['SVC poly']['gamma'],
                    random_state=42),
                'KNeighborsClassifier': KNeighborsClassifier(
                    n_neighbors=conf_file['KNN']['n_neighbors'],
                    weights=conf_file['KNN']['weight'])
            }

            estimators = [(name, model) for name, model in models.items()]
            if len(conf_file['features_tau']) == 1:
                classifier = SGDClassifier(random_state=42)
            elif conf_file['ensemble'] == 0:
                classifier = VotingClassifier(estimators=estimators, voting='hard', n_jobs=-1)
            else:
                classifier = StackingClassifier(
                    estimators=estimators,
                    n_jobs=-1,
                    final_estimator=LogisticRegression(max_iter=1000)
                )

            try:
                importance_result = compute_importance(
                    x_train, x_test, y_train, y_test,
                    classifier, conf_file['features_tau']
                )
                for feat, imp in importance_result:
                    print(feat, imp)
                    importance_sums[feat].append(imp)
            except Exception as e:
                print(f'  > Error computing permutatio importance: {e}')
                continue

        except Exception as e:
            print(f'  > Fallo inesperado en la solución {sol_id}: {e}')
            continue
