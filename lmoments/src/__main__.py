import json
import os
import sys

from compute import main as compute_main


def parse_input(input_string):
    """
    Parse the input string and extract features, ensemble bits, and integers.

    :param input_string: Input string to parse.
    :return: Tuple of parsed features, ensemble bits, and integers.
    """
    parts = input_string.split('=')
    if len(parts) != 2:
        raise ValueError("Input string must be in the format '<bits>-<bits>-<numbers> = <file>'")

    feature_bits, ensemble_bits, integers = parts[0].split('-')
    base_file = parts[1].strip()

    features = list(feature_bits.strip())
    ensemble = list(map(int, ensemble_bits.strip()))
    integers = list(map(int, integers.strip().split(',')))

    return features, ensemble, integers, base_file


def load_config(base_file):
    """
    Load configuration JSON from file.

    :param base_file: Configuration file name.
    :param dirname: Directory path containing the file.
    :return: Dictionary of loaded configuration.
    """
    conf_path = base_file
    if not os.path.isfile(conf_path):
        raise FileNotFoundError(f"Configuration file '{base_file}' not found.")

    with open(conf_path, 'r') as f:
        return json.load(f)


def save_config(config, base_file):
    """
    Save the processed configuration to a JSON file.

    :param config: Configuration dictionary.
    :param base_file: Output file name.
    :param dirname: Directory path for saving the file.
    """
    processed_path = 'lmoments/conf_processed/' + base_file.split('/')[-1]
    with open(processed_path, 'w') as outfile:
        json.dump(config, outfile)


def main():
    if len(sys.argv) > 1:
        input_string = sys.argv[1]
    else:
        print("DEBUG MODE: Using default input.")
        input_string = ('100001101101011010111000100001100111111100100110101000000001101101111111111001001111011111110000000000101100110010100110101001100110100101111111111010010'
                        ' - 000001'
                        ' - 49,2,5'
                        ' = lmoments/conf_default/CIC-DDoS2019-03-11_Syn.json')

    try:
        features, ensemble_bits, integers, base_file = parse_input(input_string)
        # for f in range(len(features)):
        #     features[f] = '0'
        # features[0] = '1'
    except ValueError as e:
        print(f"Error parsing input: {e}")
        sys.exit(1)

    try:
        config = load_config(base_file)
    except FileNotFoundError as e:
        print(e)
        sys.exit(1)

    list_features_tau = []
    for i in range(0, len(features) - 1):
        if features[i] == '1':
            base_feature = config.get('features', [])[i // 3]
            if i % 3 == 0:
                list_features_tau.append(f'{base_feature} (tau3)')
            elif i % 3 == 1:
                list_features_tau.append(f'{base_feature} (tau4)')
            else:
                list_features_tau.append(f'{base_feature} (tau5)')

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

    save_config(config, base_file)
    compute_main(base_file)
    os.remove('lmoments/conf_processed/' + base_file.split('/')[-1])


if __name__ == '__main__':
    main()
