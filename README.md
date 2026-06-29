<!--
SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>

SPDX-License-Identifier: MPL-2.0
-->

[![Power Grid Model logo](https://raw.githubusercontent.com/PowerGridModel/.github/main/artwork/svg/color.svg)](#)

# Power Grid Model template repository

This is a template repository for the Power Grid Model project.
Only common and mandatory content is included in this repository.
The derived repositories should review and edit content relevant to the best practices and standards. 

## Checklist after creating a repository from this template

Update the following items manually before publishing your repository:

- [ ] Rename the repository and update all references in this file.
- [ ] Replace all `pgm-template-repo` references across the repository, especially in this README and REUSE.toml.
- [ ] Replace the project title and description in this README.
- [ ] Ensure all third-party licenses are present in [LICENSES](https://github.com/PowerGridModel/pgm-template-repo/tree/main/LICENSES).

### Optional checks

Change these if applicable.

- [ ] Replace Power Grid Model project-specific links (for example contributing, support, release, security, and code of conduct links) 
with links for your project.
- [ ] Add logos and badges corresponding to relevant pages
- [ ] Verify [LICENSE](https://github.com/PowerGridModel/pgm-template-repo/blob/main/LICENSE) copyright holder and year(s).
Also verify SPDX copyright headers in source and documentation files.
- [ ] Include documents present in [Home page of PGM org](https://github.com/PowerGridModel/.github/) in documentation if they are created.

## License

This project is licensed under the Mozilla Public License, version 2.0 - see
[LICENSE](https://github.com/PowerGridModel/pgm-template-repo/blob/main/LICENSE) for details.

## Licenses third-party libraries

This project includes third-party libraries,
which are licensed under their own respective Open-Source licenses.
SPDX-License-Identifier headers are used to show which license is applicable.
The concerning license files can be found in the
[LICENSES](https://github.com/PowerGridModel/pgm-template-repo/tree/main/LICENSES) directory.

## Contributing

Please read [CODE_OF_CONDUCT](https://github.com/PowerGridModel/.github/blob/main/CODE_OF_CONDUCT.md) and [CONTRIBUTING](https://github.com/PowerGridModel/.github/blob/main/CONTRIBUTING.md) for details on the process 
for submitting pull requests to us.

## Citations

If you are using Power Grid Model in your research work, please consider citing our library using the following
references.

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.8054429.svg)](https://zenodo.org/record/8054429)

```bibtex
@software{Xiang_PowerGridModel_power-grid-model,
  author = {Xiang, Yu and Salemink, Peter and van Westering, Werner and Bharambe, Nitish and Govers, Martinus G.H. and van den Bogaard, Jonas and Stoeller, Bram and Wang, Zhen and Guo, Jerry Jinfeng and Figueroa Manrique, Santiago and Jagutis, Laurynas and Wang, Chenguang and van Raalte, Marc and {Contributors to the LF Energy project Power Grid Model}},
  doi = {10.5281/zenodo.8054429},
  license = {MPL-2.0},
  title = {{PowerGridModel/power-grid-model}},
  url = {https://github.com/PowerGridModel/power-grid-model}
}
@inproceedings{Xiang2023,
  author = {Xiang, Yu and Salemink, Peter and Stoeller, Bram and Bharambe, Nitish and van Westering, Werner},
  booktitle={27th International Conference on Electricity Distribution (CIRED 2023)},
  title={Power grid model: a high-performance distribution grid calculation library},
  year={2023},
  volume={2023},
  number={},
  pages={1089-1093},
  keywords={},
  doi={10.1049/icp.2023.0633}
}
```

## Contact

Please read [SUPPORT](https://github.com/PowerGridModel/.github/blob/main/SUPPORT.md) for how to connect and get into
contact with the Power Grid Model project.
